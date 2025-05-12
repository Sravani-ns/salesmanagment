package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.ApproveFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.RejectFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.service.FinanceService;
import com.vehicle.salesmanagement.workflow.FinanceWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance")
@Tag(name = "Finance Management")
public class FinanceController {

    private final @Qualifier("financeWorkflowClient") WorkflowClient workflowClient;
    private final FinanceService financeService;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate finance workflow", description = "Starts a finance workflow for a vehicle order")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Finance workflow started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or order not in BLOCKED status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<FinanceResponse>> initiateFinance(@Valid @RequestBody FinanceRequest financeRequest) {
        log.info("Initiating finance workflow for order ID: {}", financeRequest.getCustomerOrderId());

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(financeRequest.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + financeRequest.getCustomerOrderId()));
        if (orderDetails.getOrderStatus() != OrderStatus.BLOCKED) {
            log.error("Order ID: {} is not in BLOCKED status, current status: {}", financeRequest.getCustomerOrderId(), orderDetails.getOrderStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Finance initiation failed: Order must be in BLOCKED status",
                    null
            ));
        }

        FinanceResponse financeResponse;
        try {
            financeResponse = financeService.createFinanceDetails(financeRequest);
        } catch (RuntimeException e) {
            log.error("Failed to initiate finance workflow for order ID: {}: {}", financeRequest.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to initiate finance: " + e.getMessage(),
                    null
            ));
        }

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("finance-task-queue")
                .setWorkflowId("finance-" + financeRequest.getCustomerOrderId())
                .build();

        FinanceWorkflow workflow = workflowClient.newWorkflowStub(FinanceWorkflow.class, options);
        WorkflowClient.start(workflow::processFinance, financeRequest);

        try {
            String financeResponseJson = objectMapper.writeValueAsString(financeResponse);
            log.info("Finance workflow initiated successfully for order ID: {}, stored finance details: {}",
                    financeRequest.getCustomerOrderId(), financeResponseJson);
        } catch (Exception e) {
            log.error("Failed to serialize finance details for logging: {}", e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Finance workflow started successfully",
                financeResponse
        ));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve finance for an order", description = "Approves the finance workflow for a vehicle order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<FinanceResponse>> approveFinance(@Valid @RequestBody ApproveFinanceRequest request) {
        log.info("Approving finance for order ID: {}", request.getCustomerOrderId());

        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub("finance-" + request.getCustomerOrderId());
            workflow.signal("approveFinance", request.getApprovedBy());

            FinanceResponse financeResponse = workflow.getResult(FinanceResponse.class);
            log.info("Finance approved, order status set to ALLOTTED for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Finance approved successfully",
                    financeResponse
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance: Workflow execution failed for workflowId='finance-" + request.getCustomerOrderId() + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to approve finance for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance: " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject finance for an order", description = "Rejects the finance workflow for a vehicle order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<FinanceResponse>> rejectFinance(@Valid @RequestBody RejectFinanceRequest request) {
        log.info("Rejecting finance for order ID: {}", request.getCustomerOrderId());

        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub("finance-" + request.getCustomerOrderId());
            workflow.signal("rejectFinance", request.getRejectedBy());

            FinanceResponse financeResponse = workflow.getResult(FinanceResponse.class);
            log.info("Finance rejected, order status set to PENDING for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Finance rejected successfully",
                    financeResponse
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance: Workflow execution failed for workflowId='finance-" + request.getCustomerOrderId() + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to reject finance for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance: " + e.getMessage(),
                    null
            ));
        }
    }
}