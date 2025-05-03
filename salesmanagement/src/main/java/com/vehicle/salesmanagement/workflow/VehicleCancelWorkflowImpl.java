package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.VehicleOrderActivities;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class VehicleCancelWorkflowImpl implements VehicleCancelWorkflow {

    private final VehicleOrderActivities activities;

    public VehicleCancelWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.activities = io.temporal.workflow.Workflow.newActivityStub(VehicleOrderActivities.class, options);
    }

    @Override
    public OrderResponse startCancelOrder(Long customerOrderId) {
        log.info("Starting cancellation workflow for order ID: {}", customerOrderId);
        try {
            OrderResponse response = activities.cancelOrder(customerOrderId);
            log.info("Cancellation workflow completed for order ID: {}", customerOrderId);
            return response;
        } catch (Exception e) {
            log.error("Cancellation workflow failed for order ID: {} - {}", customerOrderId, e.getMessage(), e);
            throw new RuntimeException("Cancellation workflow failed: " + e.getMessage(), e);
        }
    }
}