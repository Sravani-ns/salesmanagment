package com.vehicle.salesmanagement.config;

import com.vehicle.salesmanagement.activity.VehicleOrderActivitiesImpl;
import com.vehicle.salesmanagement.workflow.VehicleCancelWorkflowImpl;
import com.vehicle.salesmanagement.workflow.VehicleOrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalWorkerConfig {

    private final VehicleOrderActivitiesImpl vehicleOrderActivities;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Creating WorkflowServiceStubs for Temporal connection");
        try {
            return WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget("localhost:7233")
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create WorkflowServiceStubs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        log.info("Creating WorkflowClient for Vehicle Order");
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating WorkerFactory for Vehicle Order");
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker vehicleOrderWorker(WorkerFactory workerFactory) {
        log.info("Configuring Temporal worker for task queue: vehicle-order-task-queue");
        try {
            Worker worker = workerFactory.newWorker("vehicle-order-task-queue");
            worker.registerWorkflowImplementationTypes(
                    VehicleOrderWorkflowImpl.class,
                    VehicleCancelWorkflowImpl.class
            );
            worker.registerActivitiesImplementations(vehicleOrderActivities);
            workerFactory.start();
            log.info("Temporal worker configured and started successfully for task queue: vehicle-order-task-queue");
            return worker;
        } catch (Exception e) {
            log.error("Failed to configure or start Temporal worker: {}", e.getMessage(), e);
            throw new RuntimeException("Worker configuration failed", e);
        }
    }
}