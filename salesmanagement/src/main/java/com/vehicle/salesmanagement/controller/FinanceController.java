package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.ApproveFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.RejectFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.UnifiedWorkflowResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.service.FinanceService;
import com.vehicle.salesmanagement.service.RedisService;
import com.vehicle.salesmanagement.workflow.UnifiedVehicleOrderWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance")
@Tag(name = "Finance Management")
public class FinanceController {

    private final @Qualifier("workflowClient") WorkflowClient workflowClient;
    private final FinanceService financeService;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate finance process", description = "Initiates the finance process within the unified workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Finance process initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or order not in BLOCKED status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> initiateFinance(@Valid @RequestBody FinanceRequest financeRequest) {
        log.info("Initiating finance process for order ID: {}", financeRequest.getCustomerOrderId());

        // Check Redis for cached state
        UnifiedWorkflowResponse cachedResponse = redisService.getWorkflowState(financeRequest.getCustomerOrderId());
        if (cachedResponse != null && cachedResponse.getFinanceResponse() != null) {
            log.info("Returning cached finance state for order ID: {}", financeRequest.getCustomerOrderId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.ACCEPTED.value(),
                    "Finance process already initiated, retrieved from cache",
                    cachedResponse
            ));
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(financeRequest.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + financeRequest.getCustomerOrderId()));
        if (orderDetails.getOrderStatus() != OrderStatus.BLOCKED) {
            log.error("Order ID: {} is not in BLOCKED status, current status: {}", financeRequest.getCustomerOrderId(), orderDetails.getOrderStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Finance initiation failed: Order must be in BLOCKED status",
                    null
            ));
        }

        FinanceResponse financeResponse;
        try {
            financeResponse = financeService.createFinanceDetails(financeRequest);
        } catch (RuntimeException e) {
            log.error("Failed to initiate finance process for order ID: {}: {}", financeRequest.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to initiate finance: " + e.getMessage(),
                    null
            ));
        }

        // Signal the unified workflow (assuming it's already running)
        String workflowId = "unified-" + financeRequest.getCustomerOrderId();
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            workflow.signal("initiateFinance", financeRequest);
            log.info("Finance process initiated within unified workflow for order ID: {}", financeRequest.getCustomerOrderId());
        } catch (Exception e) {
            log.warn("Failed to signal unified workflow for order ID: {}: {}", financeRequest.getCustomerOrderId(), e.getMessage());
        }

        try {
            String financeResponseJson = objectMapper.writeValueAsString(financeResponse);
            log.info("Finance process initiated successfully for order ID: {}, stored finance details: {}", financeRequest.getCustomerOrderId(), financeResponseJson);
        } catch (Exception e) {
            log.error("Failed to serialize finance details for logging: {}", e.getMessage());
        }

        // Fetch the current state of the unified workflow
        UnifiedWorkflowResponse unifiedResponse;
        try {
            unifiedResponse = workflowClient.newUntypedWorkflowStub(workflowId)
                    .getResult(10, TimeUnit.SECONDS, UnifiedWorkflowResponse.class);
            redisService.cacheWorkflowState(financeRequest.getCustomerOrderId(), unifiedResponse);
        } catch (Exception e) {
            log.warn("Failed to get unified workflow result for order ID: {}: {}", financeRequest.getCustomerOrderId(), e.getMessage());
            unifiedResponse = new UnifiedWorkflowResponse(null, financeResponse, null, null);
            redisService.cacheWorkflowState(financeRequest.getCustomerOrderId(), unifiedResponse);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Finance process initiated successfully",
                unifiedResponse
        ));
    }

    @PostMapping("/approve")
    @Operation(summary = "Approve finance for an order", description = "Approves the finance process within the unified workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> approveFinance(@Valid @RequestBody ApproveFinanceRequest request) {
        log.info("Approving finance for order ID: {}", request.getCustomerOrderId());

        // Cache the approval signal in Redis
        redisService.cacheSignal("finance_approve", request.getCustomerOrderId(), true);
        redisService.cacheSignal("finance_approved_by", request.getCustomerOrderId(), request.getApprovedBy());

        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub("unified-" + request.getCustomerOrderId());
            workflow.signal("approveFinance", request.getApprovedBy());

            UnifiedWorkflowResponse unifiedResponse = workflow.getResult(UnifiedWorkflowResponse.class);
            redisService.cacheWorkflowState(request.getCustomerOrderId(), unifiedResponse);
            log.info("Finance approved, order status set to ALLOTTED for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Finance approved successfully",
                    unifiedResponse
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance: Workflow execution failed for workflowId='unified-" + request.getCustomerOrderId() + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to approve finance for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance: " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject finance for an order", description = "Rejects the finance process within the unified workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> rejectFinance(@Valid @RequestBody RejectFinanceRequest request) {
        log.info("Rejecting finance for order ID: {}", request.getCustomerOrderId());

        // Cache the rejection signal in Redis
        redisService.cacheSignal("finance_reject", request.getCustomerOrderId(), true);
        redisService.cacheSignal("finance_rejected_by", request.getCustomerOrderId(), request.getRejectedBy());

        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub("unified-" + request.getCustomerOrderId());
            workflow.signal("rejectFinance", request.getRejectedBy());

            UnifiedWorkflowResponse unifiedResponse = workflow.getResult(UnifiedWorkflowResponse.class);
            redisService.cacheWorkflowState(request.getCustomerOrderId(), unifiedResponse);
            log.info("Finance rejected, order status set to PENDING for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Finance rejected successfully",
                    unifiedResponse
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance: Workflow execution failed for workflowId='unified-" + request.getCustomerOrderId() + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to reject finance for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance: " + e.getMessage(),
                    null
            ));
        }
    }
}