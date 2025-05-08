package com.vehicle.salesmanagement.config;

import com.vehicle.salesmanagement.activity.DispatchDeliveryActivitiesImpl;
import com.vehicle.salesmanagement.activity.FinanceActivitiesImpl;
import com.vehicle.salesmanagement.activity.VehicleOrderActivitiesImpl;
import com.vehicle.salesmanagement.service.RedisService;
import com.vehicle.salesmanagement.workflow.UnifiedVehicleOrderWorkflowImpl;
import com.vehicle.salesmanagement.workflow.VehicleCancelWorkflowImpl;
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

    private final RedisService redisService;
    private final VehicleOrderActivitiesImpl vehicleOrderActivities;
    private final FinanceActivitiesImpl financeActivities;
    private final DispatchDeliveryActivitiesImpl dispatchDeliveryActivities;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("localhost:7233") // Adjust host/port as needed
                        .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker unifiedWorker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker("unified-task-queue");

        // Register the unified workflow
        worker.registerWorkflowImplementationTypes(
                UnifiedVehicleOrderWorkflowImpl.class,
                VehicleCancelWorkflowImpl.class
        );

        // Register activities using injected instances
        worker.registerActivitiesImplementations(
                vehicleOrderActivities,
                financeActivities,
                dispatchDeliveryActivities
        );

        log.info("Unified worker registered with task queue: unified-task-queue");
        return worker;
    }

    @Bean
    public WorkerFactory startWorkerFactory(WorkerFactory workerFactory) {
        workerFactory.start();
        log.info("Worker factory started");
        return workerFactory;
    }
}