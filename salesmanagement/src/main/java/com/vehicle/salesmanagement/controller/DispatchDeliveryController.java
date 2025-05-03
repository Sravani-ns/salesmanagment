package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.service.DispatchDeliveryService;
import com.vehicle.salesmanagement.workflow.DispatchDeliveryWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dispatch-delivery")
@Tag(name = "Dispatch and Delivery Management")
public class DispatchDeliveryController {

    private final @Qualifier("dispatchDeliveryWorkflowClient") WorkflowClient workflowClient;
    private final DispatchDeliveryService dispatchDeliveryService;

    @PostMapping("/initiate-dispatch")
    @Operation(summary = "Initiate dispatch workflow", description = "Starts a dispatch workflow for a vehicle order")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Dispatch workflow started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<DispatchResponse> initiateDispatch(@Valid @RequestBody DispatchRequest dispatchRequest) {
        log.info("Initiating dispatch workflow for order ID: {}", dispatchRequest.getCustomerOrderId());

        DispatchResponse dispatchResponse;
        try {
            dispatchResponse = dispatchDeliveryService.initiateDispatch(dispatchRequest);
        } catch (Exception e) {
            log.error("Failed to initiate dispatch for order ID: {}: {}", dispatchRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to initiate dispatch: " + e.getMessage(),
                    null
            );
        }

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("dispatch-delivery-task-queue")
                .setWorkflowId("dispatch-delivery-" + dispatchRequest.getCustomerOrderId())
                .build();

        DispatchDeliveryWorkflow workflow = workflowClient.newWorkflowStub(DispatchDeliveryWorkflow.class, options);
        WorkflowClient.start(workflow::processDispatchAndDelivery, dispatchRequest);

        log.info("Dispatch workflow started, vehicle order details updated to DISPATCHED for order ID: {}", dispatchRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Dispatch workflow started for order ID: " + dispatchRequest.getCustomerOrderId(),
                dispatchResponse
        );
    }

    @PostMapping("/confirm-delivery")
    @Operation(summary = "Confirm vehicle delivery", description = "Confirms delivery and updates order status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<DeliveryResponse> confirmDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest) {
        log.info("Confirming delivery for order ID: {}", deliveryRequest.getCustomerOrderId());

        DeliveryResponse deliveryResponse;
        try {
            deliveryResponse = dispatchDeliveryService.confirmDelivery(deliveryRequest);
        } catch (IllegalStateException e) {
            log.error("Failed to confirm delivery for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to confirm delivery: " + e.getMessage(),
                    null
            );
        } catch (Exception e) {
            log.error("Failed to confirm delivery for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to confirm delivery: " + e.getMessage(),
                    null
            );
        }

        String workflowId = "dispatch-delivery-" + deliveryRequest.getCustomerOrderId();
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            workflow.signal("confirmDelivery", deliveryRequest);
            log.info("Delivery confirmation signal sent for order ID: {}", deliveryRequest.getCustomerOrderId());
        } catch (WorkflowNotFoundException e) {
            log.warn("Workflow not found for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but workflow not found for workflowId='" + workflowId + "'",
                    deliveryResponse
            );
        } catch (Exception e) {
            log.error("Failed to signal delivery confirmation for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but failed to signal workflow: " + e.getMessage(),
                    deliveryResponse
            );
        }

        log.info("Delivery confirmed, vehicle order details updated to DELIVERED for order ID: {}", deliveryRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                "Delivery confirmed successfully",
                deliveryResponse
        );
    }
}