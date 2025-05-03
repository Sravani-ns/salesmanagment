package com.vehicle.salesmanagement.controller;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.StockAddRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleModelRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleVariantRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleModelResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleModelRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import com.vehicle.salesmanagement.service.VehicleOrderService;
import com.vehicle.salesmanagement.workflow.VehicleOrderWorkflow;
import com.vehicle.salesmanagement.workflow.VehicleCancelWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Vehicle Order Management")
public class VehicleOrderController {

    private final @Qualifier("workflowClient") WorkflowClient workflowClient;
    private final VehicleOrderDetailsRepository orderRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleVariantRepository vehicleVariantRepository;
    private final VehicleOrderService vehicleOrderService;

    @Transactional
    @PostMapping("/create")
    @Operation(summary = "Place a new vehicle order", description = "Initiates a vehicle order and starts the order workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        log.info("Received OrderRequest: {}", orderRequest);
        VehicleOrderDetails orderDetails = mapOrderRequestToEntity(orderRequest);
        orderDetails.setCreatedAt(LocalDateTime.now());
        orderDetails.setUpdatedAt(LocalDateTime.now());
        orderDetails = orderRepository.saveAndFlush(orderDetails);
        log.info("Order saved with ID: {}", orderDetails.getCustomerOrderId());

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("vehicle-order-task-queue")
                .setWorkflowId("order-" + orderDetails.getCustomerOrderId())
                .build();

        VehicleOrderWorkflow workflow = workflowClient.newWorkflowStub(VehicleOrderWorkflow.class, options);
        WorkflowClient.start(workflow::placeOrder, orderRequest);

        OrderResponse response;
        try {
            response = workflowClient.newUntypedWorkflowStub("order-" + orderDetails.getCustomerOrderId())
                    .getResult(30, TimeUnit.SECONDS, OrderResponse.class);
        } catch (Exception e) {
            log.error("Failed to get workflow result for order ID: {} - {}", orderDetails.getCustomerOrderId(), e.getMessage());
            response = vehicleOrderService.mapToOrderResponse(orderRequest);
            response.setOrderStatus(OrderStatus.PENDING);
            response.setCustomerName(orderRequest.getCustomerName());
            response.setModelName(orderRequest.getModelName());
            response.setCreatedAt(LocalDateTime.now());
            response.setCustomerOrderId(orderDetails.getCustomerOrderId());
        }

        if (response.getOrderStatus() == null) {
            log.warn("Workflow returned null status for order ID: {}, defaulting to PENDING", orderDetails.getCustomerOrderId());
            response.setOrderStatus(OrderStatus.PENDING);
        }

        orderDetails.setOrderStatus(response.getOrderStatus());
        orderRepository.save(orderDetails);

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<OrderResponse> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Order placed successfully with ID: " + orderDetails.getCustomerOrderId() + ". Workflow started.",
                response
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel a vehicle order", description = "Cancels an existing vehicle order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<OrderResponse>> cancelOrder(@Valid @RequestParam Long customerOrderId) {
        log.info("Canceling order with ID: {}", customerOrderId);
        try {
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("vehicle-order-task-queue")
                    .setWorkflowId("cancel-order-" + customerOrderId)
                    .build();

            VehicleCancelWorkflow workflow = workflowClient.newWorkflowStub(VehicleCancelWorkflow.class, options);
            WorkflowClient.start(workflow::startCancelOrder, customerOrderId);

            OrderResponse response = workflowClient.newUntypedWorkflowStub("cancel-order-" + customerOrderId)
                    .getResult(10, TimeUnit.SECONDS, OrderResponse.class);

            log.info("Cancellation workflow started and completed successfully for order ID: {}", customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<OrderResponse> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with ID: " + customerOrderId,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to start or complete cancellation workflow for order ID: {} - {}", customerOrderId, e.getMessage());
            OrderResponse response = vehicleOrderService.cancelOrder(customerOrderId);
            log.info("Fallback: Order canceled directly with ID: {}", customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<OrderResponse> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with ID: " + customerOrderId + " (via fallback)",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        }
    }

    @PostMapping("/addStock")
    @Operation(summary = "Add vehicle stock", description = "Adds stock for a vehicle model and variant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<String>> addStock(@Valid @RequestBody StockAddRequest request) {
        vehicleOrderService.addVehicleStock(
                request.getModelId(),
                request.getVariantId(),
                request.getSuffix(),
                request.getFuelType(),
                request.getColour(),
                request.getEngineColour(),
                request.getTransmissionType(),
                request.getVariantName(),
                request.getQuantity(),
                request.getInteriorColour(),
                request.getVinNumber(),
                request.getCreatedBy()
        );
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<String> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                "Stock added successfully",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/addVehicleModel")
    @Operation(summary = "Add a vehicle model", description = "Adds a new vehicle model to the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle model added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<VehicleModelResponse>> addModel(@Valid @RequestBody VehicleModelRequest request) {
        VehicleModelResponse modelResponse = vehicleOrderService.addVehicleModel(request);
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<VehicleModelResponse> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                modelResponse.getMessage() != null ? modelResponse.getMessage() : "Vehicle model added successfully with ID: " + modelResponse.getVehicleModelId(),
                modelResponse
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/addVariant")
    @Operation(summary = "Add a vehicle variant", description = "Adds a new variant to current vehicle model")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variant added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<VehicleVariant>> addVariant(@Valid @RequestBody VehicleVariantRequest request) {
        VehicleVariant saved = vehicleOrderService.addVariantToModel(request);
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<VehicleVariant> apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                "Variant added with ID: " + saved.getVehicleVariantId(),
                saved
        );
        return ResponseEntity.ok(apiResponse);
    }

    private VehicleOrderDetails mapOrderRequestToEntity(OrderRequest request) {
        VehicleOrderDetails order = new VehicleOrderDetails();
        order.setVehicleModel(vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + request.getVehicleModelId())));
        order.setVehicleVariant(vehicleVariantRepository.findById(request.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + request.getVehicleVariantId())));
        order.setCustomerName(request.getCustomerName());
        order.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "");
        order.setEmail(request.getEmail() != null ? request.getEmail() : "");
        order.setPermanentAddress(request.getPermanentAddress() != null ? request.getPermanentAddress() : "");
        order.setCurrentAddress(request.getCurrentAddress() != null ? request.getCurrentAddress() : "");
        order.setAadharNo(request.getAadharNo() != null ? request.getAadharNo() : "");
        order.setPanNo(request.getPanNo() != null ? request.getPanNo() : "");
        order.setModelName(request.getModelName());
        order.setFuelType(request.getFuelType());
        order.setColour(request.getColour());
        order.setTransmissionType(request.getTransmissionType());
        order.setVariant(request.getVariant());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice().doubleValue()));
        order.setBookingAmount(BigDecimal.valueOf(request.getBookingAmount().doubleValue()));
        order.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "");
        order.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system");
        order.setUpdatedBy(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}