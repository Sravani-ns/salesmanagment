package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleModelRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import com.vehicle.salesmanagement.service.VehicleOrderService;
import com.vehicle.salesmanagement.workflow.VehicleCancelWorkflow;
import com.vehicle.salesmanagement.workflow.VehicleOrderWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.transaction.Transactional;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final ObjectMapper objectMapper;

    @Transactional
    @PostMapping("/create")
    @Operation(summary = "Place vehicle order(s)", description = "Initiates one or multiple vehicle orders and starts workflows for each")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order(s) placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> placeOrder(@RequestBody Object request) {
        try {
            String rawRequest = objectMapper.writeValueAsString(request);
            log.info("Received raw request: {}", rawRequest);

            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            if (request instanceof LinkedHashMap || request instanceof Map) {
                MultiOrderRequest multiOrderRequest = objectMapper.convertValue(request, MultiOrderRequest.class);
                if (multiOrderRequest.getVehicleOrders() != null && !multiOrderRequest.getVehicleOrders().isEmpty()) {
                    for (OrderRequest order : multiOrderRequest.getVehicleOrders()) {
                        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(order);
                        if (!violations.isEmpty()) {
                            throw new ConstraintViolationException(violations);
                        }
                    }
                    log.info("Deserialized as MultiOrderRequest with {} orders", multiOrderRequest.getVehicleOrders().size());
                    return handleMultiOrder(multiOrderRequest);
                }

                OrderRequest orderRequest = objectMapper.convertValue(request, OrderRequest.class);
                Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                log.info("Deserialized as OrderRequest");
                return handleSingleOrder(orderRequest);
            }

            if (request instanceof OrderRequest orderRequest) {
                Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                log.info("Received single OrderRequest: {}", orderRequest);
                return handleSingleOrder(orderRequest);
            } else if (request instanceof MultiOrderRequest multiOrderRequest) {
                for (OrderRequest order : multiOrderRequest.getVehicleOrders()) {
                    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(order);
                    if (!violations.isEmpty()) {
                        throw new ConstraintViolationException(violations);
                    }
                }
                log.info("Received MultiOrderRequest with {} vehicle orders", multiOrderRequest.getVehicleOrders().size());
                return handleMultiOrder(multiOrderRequest);
            } else {
                log.error("Invalid request type: {}", request.getClass().getName());
                throw new IllegalArgumentException("Request must be either OrderRequest or MultiOrderRequest");
            }
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.error("Validation failed: {}", errorMessage);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Validation error: " + errorMessage,
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Deserialization or validation failed: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid request: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error processing request: {}", e.getMessage(), e);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> handleSingleOrder(@Valid OrderRequest orderRequest) {
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

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Order placed successfully with ID: " + orderDetails.getCustomerOrderId() + ". Workflow started.",
                response
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    private ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> handleMultiOrder(@Valid MultiOrderRequest multiOrderRequest) {
        List<OrderResponse> orderResponses = new ArrayList<>();
        List<Long> orderIds = new ArrayList<>();

        for (OrderRequest orderRequest : multiOrderRequest.getVehicleOrders()) {
            VehicleOrderDetails orderDetails = mapOrderRequestToEntity(orderRequest);
            orderDetails.setCreatedAt(LocalDateTime.now());
            orderDetails.setUpdatedAt(LocalDateTime.now());
            orderDetails = orderRepository.saveAndFlush(orderDetails);
            orderIds.add(orderDetails.getCustomerOrderId());
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
            orderResponses.add(response);
        }

        MultiOrderResponse multiOrderResponse = new MultiOrderResponse(
                HttpStatus.ACCEPTED.value(),
                "Orders placed successfully with IDs: " + orderIds,
                orderResponses
        );

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Multiple orders placed successfully. Workflows started.",
                multiOrderResponse
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
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> cancelOrder(@Valid @RequestParam Long customerOrderId) {
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
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with ID: " + customerOrderId,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to start or complete cancellation workflow for order ID: {} - {}", customerOrderId, e.getMessage());
            OrderResponse response = vehicleOrderService.cancelOrder(customerOrderId);
            log.info("Fallback: Order canceled directly with ID: {}", customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
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
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> addStock(@Valid @RequestBody StockAddRequest request) {
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
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
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
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> addModel(@Valid @RequestBody VehicleModelRequest request) {
        VehicleModelResponse modelResponse = vehicleOrderService.addVehicleModel(request);
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
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
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> addVariant(@Valid @RequestBody VehicleVariantRequest request) {
        VehicleVariant saved = vehicleOrderService.addVariantToModel(request);
        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
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

    @GetMapping("/{customerOrderId}/vehicle-count")
    @Operation(summary = "Get booked vehicle count", description = "Retrieves the number of vehicles booked for a given customer order ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle count retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customer order ID"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getBookedVehicleCount(@PathVariable Long customerOrderId) {
        try {
            if (customerOrderId == null || customerOrderId <= 0) {
                throw new IllegalArgumentException("Customer order ID must be a positive number");
            }
            int vehicleCount = vehicleOrderService.getBookedVehicleCount(customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Vehicle count retrieved successfully for order ID: " + customerOrderId,
                    vehicleCount
            );
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Invalid customer order ID: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid request: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (RuntimeException e) {
            log.error("Order not found for ID: {} - {}", customerOrderId, e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.NOT_FOUND.value(),
                    e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving vehicle count for order ID: {} - {}", customerOrderId, e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }


    @GetMapping("/total")
    @Operation(summary = "Get total orders", description = "Retrieves the total number of orders in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total orders retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getTotalOrders() {
        log.info("Received request for total orders");
        try {
            TotalOrdersResponse response = vehicleOrderService.getTotalOrders();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Total orders retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving total orders: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Get pending orders count", description = "Retrieves the total number of orders with PENDING status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending orders count retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getPendingOrdersCount() {
        log.info("Received request for pending orders count");
        try {
            TotalOrdersResponse response = vehicleOrderService.getPendingOrdersCount();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Pending orders count retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving pending orders count: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }


    @GetMapping("/finance-pending/count")
    @Operation(summary = "Get finance pending orders count", description = "Retrieves the total number of orders with PENDING_FINANCE status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance pending orders count retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getFinancePendingOrdersCount() {
        log.info("Received request for finance pending orders count");
        try {
            TotalOrdersResponse response = vehicleOrderService.getFinancePendingOrdersCount();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Finance pending orders count retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving finance pending orders count: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/closed/count")
    @Operation(summary = "Get closed orders count", description = "Retrieves the total number of orders with CLOSED status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Closed orders count retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getClosedOrdersCount() {
        log.info("Received request for closed orders count");
        try {
            TotalOrdersResponse response = vehicleOrderService.getClosedOrdersCount();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Closed orders count retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving closed orders count: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }




    @GetMapping("/{customerOrderId}")
    @Operation(summary = "Get order details by customer order ID", description = "Retrieves specific details of a vehicle order by customer order ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid customer order ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getOrderDetailsByCustomerOrderId(@PathVariable Long customerOrderId) {
        log.info("Received request to fetch order details for customer order ID: {}", customerOrderId);
        try {
            if (customerOrderId == null || customerOrderId <= 0) {
                throw new IllegalArgumentException("Customer order ID must be a positive number");
            }

            OrderDetailsResponse response = vehicleOrderService.getOrderDetailsByCustomerOrderId(customerOrderId);

            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order details retrieved successfully for customer order ID: " + customerOrderId,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Invalid customer order ID: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid request: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (RuntimeException e) {
            log.error("Order not found for customer order ID: {} - {}", customerOrderId, e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.NOT_FOUND.value(),
                    e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving order details for customer order ID: {} - {}", customerOrderId, e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}