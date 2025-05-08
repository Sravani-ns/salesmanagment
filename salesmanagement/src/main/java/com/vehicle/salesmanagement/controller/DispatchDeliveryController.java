package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.UnifiedWorkflowResponse;
import com.vehicle.salesmanagement.service.DispatchDeliveryService;
import com.vehicle.salesmanagement.service.RedisService;
import com.vehicle.salesmanagement.workflow.UnifiedVehicleOrderWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
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

    private final @Qualifier("workflowClient") WorkflowClient workflowClient;
    private final DispatchDeliveryService dispatchDeliveryService;
    private final RedisService redisService;

    @PostMapping("/initiate-dispatch")
    @Operation(summary = "Initiate dispatch process", description = "Initiates the dispatch process within the unified workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Dispatch process started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<UnifiedWorkflowResponse> initiateDispatch(@Valid @RequestBody DispatchRequest dispatchRequest) {
        log.info("Initiating dispatch process for order ID: {}", dispatchRequest.getCustomerOrderId());

        // Check Redis for cached state
        UnifiedWorkflowResponse cachedResponse = redisService.getWorkflowState(dispatchRequest.getCustomerOrderId());
        if (cachedResponse != null && cachedResponse.getDispatchResponse() != null) {
            log.info("Returning cached dispatch state for order ID: {}", dispatchRequest.getCustomerOrderId());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.ACCEPTED.value(),
                    "Dispatch process already initiated, retrieved from cache",
                    cachedResponse
            );
        }

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

        String workflowId = "unified-" + dispatchRequest.getCustomerOrderId();
        UnifiedWorkflowResponse unifiedResponse;
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            // Signal the unified workflow (already running)
            workflow.signal("initiateDispatch", dispatchRequest);
            unifiedResponse = workflow.getResult(UnifiedWorkflowResponse.class);
            redisService.cacheWorkflowState(dispatchRequest.getCustomerOrderId(), unifiedResponse);
            log.info("Dispatch process initiated within unified workflow for order ID: {}", dispatchRequest.getCustomerOrderId());
        } catch (Exception e) {
            log.warn("Failed to signal or get result from unified workflow for order ID: {}: {}", dispatchRequest.getCustomerOrderId(), e.getMessage());
            unifiedResponse = new UnifiedWorkflowResponse(null, null, dispatchResponse, null);
            redisService.cacheWorkflowState(dispatchRequest.getCustomerOrderId(), unifiedResponse);
        }

        log.info("Dispatch process started, vehicle order details updated to DISPATCHED for order ID: {}", dispatchRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Dispatch process started for order ID: " + dispatchRequest.getCustomerOrderId(),
                unifiedResponse
        );
    }

    @PostMapping("/confirm-delivery")
    @Operation(summary = "Confirm vehicle delivery", description = "Confirms delivery and updates order status within the unified workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<UnifiedWorkflowResponse> confirmDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest) {
        log.info("Confirming delivery for order ID: {}", deliveryRequest.getCustomerOrderId());

        // Cache the delivery confirmation signal in Redis
        redisService.cacheSignal("delivery_confirm", deliveryRequest.getCustomerOrderId(), deliveryRequest);

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

        String workflowId = "unified-" + deliveryRequest.getCustomerOrderId();
        UnifiedWorkflowResponse unifiedResponse;
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            workflow.signal("confirmDelivery", deliveryRequest);
            unifiedResponse = workflow.getResult(UnifiedWorkflowResponse.class);
            redisService.cacheWorkflowState(deliveryRequest.getCustomerOrderId(), unifiedResponse);
            log.info("Delivery confirmation signal sent for order ID: {}", deliveryRequest.getCustomerOrderId());
        } catch (WorkflowNotFoundException e) {
            log.warn("Workflow not found for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            unifiedResponse = new UnifiedWorkflowResponse(null, null, null, deliveryResponse);
            redisService.cacheWorkflowState(deliveryRequest.getCustomerOrderId(), unifiedResponse);
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but workflow not found for workflowId='" + workflowId + "'",
                    unifiedResponse
            );
        } catch (Exception e) {
            log.error("Failed to signal delivery confirmation for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            unifiedResponse = new UnifiedWorkflowResponse(null, null, null, deliveryResponse);
            redisService.cacheWorkflowState(deliveryRequest.getCustomerOrderId(), unifiedResponse);
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but failed to signal workflow: " + e.getMessage(),
                    unifiedResponse
            );
        }

        log.info("Delivery confirmed, vehicle order details updated to DELIVERED for order ID: {}", deliveryRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                "Delivery confirmed successfully",
                unifiedResponse
        );
    }
}