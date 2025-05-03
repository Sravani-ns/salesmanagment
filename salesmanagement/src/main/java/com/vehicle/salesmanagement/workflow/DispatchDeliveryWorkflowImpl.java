package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.DispatchDeliveryActivities;
import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class DispatchDeliveryWorkflowImpl implements DispatchDeliveryWorkflow {

    private final DispatchDeliveryActivities activities;
    private DeliveryRequest deliveryRequest;
    private boolean isDeliveryConfirmed = false;

    public DispatchDeliveryWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.activities = Workflow.newActivityStub(DispatchDeliveryActivities.class, options);
    }

    @Override
    public DeliveryResponse processDispatchAndDelivery(DispatchRequest dispatchRequest) {
        Long orderId = dispatchRequest.getCustomerOrderId();
        log.info("Starting dispatch and delivery workflow for order ID: {}", orderId);

        // Step 1: Check order status and initiate dispatch if ALLOTTED
        Optional<VehicleOrderDetails> orderDetailsOpt = activities.getVehicleOrderDetails(orderId);
        if (orderDetailsOpt.isPresent()) {
            VehicleOrderDetails orderDetails = orderDetailsOpt.get();
            OrderStatus currentStatus = orderDetails.getOrderStatus();
            if (currentStatus.equals(OrderStatus.ALLOTTED)) {
                DispatchResponse dispatchResponse = activities.initiateDispatch(dispatchRequest);
                if (!dispatchResponse.getOrderStatus().equals(OrderStatus.DISPATCHED)) {
                    log.error("Dispatch failed to set order status to DISPATCHED for order ID: {}", orderId);
                    throw new RuntimeException("Dispatch failed for order ID: " + orderId);
                }
                log.info("Dispatch completed, order status updated to DISPATCHED for order ID: {}", orderId);
            } else if (currentStatus.equals(OrderStatus.DISPATCHED) ||
                    currentStatus.equals(OrderStatus.DELIVERED) ||
                    currentStatus.equals(OrderStatus.COMPLETED)) {
                log.warn("Order ID {} is already in status {}. Skipping dispatch.", orderId, currentStatus);
            } else {
                log.error("Order ID {} is in invalid status {} for dispatch.", orderId, currentStatus);
                throw new RuntimeException("Invalid order status for dispatch: " + currentStatus);
            }
        } else {
            log.error("Order ID {} not found for dispatch.", orderId);
            throw new RuntimeException("Order not found: " + orderId);
        }

        // Step 2: Wait for delivery confirmation signal
        Workflow.await(Duration.ofDays(7), () -> isDeliveryConfirmed);

        if (!isDeliveryConfirmed || deliveryRequest == null) {
            log.warn("Delivery not confirmed within 7 days for order ID: {}", orderId);
            throw new RuntimeException("Delivery not confirmed for order ID: " + orderId);
        }

        // Step 3: Check order status and confirm delivery if DISPATCHED
        DeliveryResponse deliveryResponse = null;
        orderDetailsOpt = activities.getVehicleOrderDetails(orderId);
        if (orderDetailsOpt.isPresent()) {
            OrderStatus currentStatus = orderDetailsOpt.get().getOrderStatus();
            if (currentStatus.equals(OrderStatus.DISPATCHED)) {
                deliveryResponse = activities.confirmDelivery(deliveryRequest);
                if (!deliveryResponse.getOrderStatus().equals(OrderStatus.DELIVERED)) {
                    log.error("Delivery confirmation failed to set order status to DELIVERED for order ID: {}", orderId);
                    throw new RuntimeException("Delivery confirmation failed for order ID: " + orderId);
                }
                log.info("Delivery completed, order status updated to DELIVERED for order ID: {}", orderId);
            } else if (currentStatus.equals(OrderStatus.DELIVERED) ||
                    currentStatus.equals(OrderStatus.COMPLETED)) {
                log.warn("Order ID {} is already in status {}. Skipping delivery confirmation.",
                        orderId, currentStatus);
                deliveryResponse = activities.confirmDelivery(deliveryRequest); // Idempotent call
            } else {
                log.error("Order ID {} is in invalid status {} for delivery.", orderId, currentStatus);
                throw new RuntimeException("Invalid order status for delivery: " + currentStatus);
            }
        } else {
            log.error("Order ID {} not found for delivery confirmation.", orderId);
            throw new RuntimeException("Order not found: " + orderId);
        }

        log.info("Dispatch and delivery workflow completed for order ID: {}", orderId);
        return deliveryResponse;
    }

    @Override
    public void confirmDelivery(DeliveryRequest deliveryRequest) {
        log.info("Received delivery confirmation signal for order ID: {}", deliveryRequest.getCustomerOrderId());
        this.deliveryRequest = deliveryRequest;
        this.isDeliveryConfirmed = true;
    }
}