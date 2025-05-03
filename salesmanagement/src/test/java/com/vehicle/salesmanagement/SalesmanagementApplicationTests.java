package com.vehicle.salesmanagement;

import com.vehicle.salesmanagement.controller.*;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.*;
import com.vehicle.salesmanagement.repository.*;
import com.vehicle.salesmanagement.service.*;
import com.vehicle.salesmanagement.workflow.FinanceWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleSalesManagementApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(VehicleSalesManagementApplicationTests.class);

    // Mocks for VehicleOrderServiceTests
    @Mock
    private StockDetailsRepository stockRepository;
    @Mock
    private MddpStockRepository mddpStockRepository;
    @Mock
    private ManufacturerOrderRepository manufacturerOrderRepository;
    @Mock
    private VehicleOrderDetailsRepository orderRepository;
    @Mock
    private VehicleVariantRepository variantRepository;
    @Mock
    private VehicleModelRepository vehicleModelRepository;
    @Mock
    private HistoryService historyService;
    @InjectMocks
    private VehicleOrderService vehicleOrderService;

    // Mocks for FinanceServiceTests
    @Mock
    private FinanceDetailsRepository financeDetailsRepository;
    @Mock
    private FinanceService financeService;
    @InjectMocks
    private FinanceService financeServiceInjected;

    // Mocks for DispatchDeliveryServiceTests
    @Mock
    private DispatchDetailsRepository dispatchDetailsRepository;
    @Mock
    private DeliveryDetailsRepository deliveryDetailsRepository;
    @InjectMocks
    private DispatchDeliveryService dispatchDeliveryService;

    // Mocks for HistoryServiceTests
    @Mock
    private VehicleOrderDetailsHistoryRepository orderHistoryRepository;
    @Mock
    private FinanceDetailsHistoryRepository financeHistoryRepository;
    @Mock
    private DispatchDetailsHistoryRepository dispatchHistoryRepository;
    @Mock
    private DeliveryDetailsHistoryRepository deliveryHistoryRepository;
    @InjectMocks
    private HistoryService historyServiceInjected;

    // Mocks for VehicleOrderControllerTests
    @Mock
    private WorkflowClient workflowClient;
    @InjectMocks
    private VehicleOrderController vehicleOrderController;

    // Mocks for FinanceControllerTests
    @InjectMocks
    private FinanceController financeController;

    // Mocks for DispatchDeliveryControllerTests
    @InjectMocks
    private DispatchDeliveryController dispatchDeliveryController;

    // Shared test data
    private OrderRequest orderRequest;
    private VehicleVariant variant;
    private StockDetails stock;
    private FinanceRequest financeRequest;
    private VehicleOrderDetails orderDetails;
    private DispatchRequest dispatchRequest;
    private DeliveryRequest deliveryRequest;

    @BeforeEach
    void setUp() {
        // Setup for VehicleOrderServiceTests
        orderRequest = new OrderRequest();
        orderRequest.setVehicleModelId(1L);
        orderRequest.setVehicleVariantId(1L);
        orderRequest.setCustomerName("John Doe");
        orderRequest.setModelName("Model X");
        orderRequest.setQuantity(1);
        orderRequest.setColour("Red");
        orderRequest.setFuelType("Petrol");
        orderRequest.setTransmissionType("Automatic");
        orderRequest.setVariant("Premium");

        variant = new VehicleVariant();
        variant.setVehicleVariantId(1L);
        variant.setPrice(new BigDecimal("50000"));

        stock = new StockDetails();
        stock.setQuantity(5);
        stock.setColour("Red");
        stock.setFuelType("Petrol");
        stock.setTransmissionType("Automatic");
        stock.setStockStatus(StockStatus.AVAILABLE);

        // Setup for FinanceServiceTests
        financeRequest = new FinanceRequest();
        financeRequest.setCustomerOrderId(1L);
        financeRequest.setCustomerName("John Doe");

        orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId(1L);
        orderDetails.setOrderStatus(OrderStatus.BLOCKED);

        // Setup for DispatchDeliveryServiceTests
        dispatchRequest = new DispatchRequest();
        dispatchRequest.setCustomerOrderId(1L);
        dispatchRequest.setDispatchedBy("admin");

        deliveryRequest = new DeliveryRequest();
        deliveryRequest.setCustomerOrderId(1L);
        deliveryRequest.setDeliveredBy("admin");
        deliveryRequest.setRecipientName("John Doe");

        orderDetails.setCustomerName("John Doe");
        orderDetails.setModelName("Model X");
        orderDetails.setVariant("Premium");
    }

    @Test
    void checkAndBlockStock_ShouldBlockAvailableStock() {
        logger.info("Starting test: checkAndBlockStock_ShouldBlockAvailableStock");
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(stockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE))
                .thenReturn(Collections.singletonList(stock));

        OrderResponse response = vehicleOrderService.checkAndBlockStock(orderRequest);

        logger.info("Order status after blocking stock: {}", response.getOrderStatus());
        assertEquals(OrderStatus.BLOCKED, response.getOrderStatus());
        verify(stockRepository).save(stock);
    }

    @Test
    void checkAndBlockStock_ShouldPlaceManufacturerOrderWhenNoStock() {
        logger.info("Starting test: checkAndBlockStock_ShouldPlaceManufacturerOrderWhenNoStock");
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(stockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

        OrderResponse response = vehicleOrderService.checkAndBlockStock(orderRequest);

        logger.info("Order status when no stock: {}", response.getOrderStatus());
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
    }

    @Test
    void checkAndReserveMddpStock_ShouldReserveWhenAvailable() {
        logger.info("Starting test: checkAndReserveMddpStock_ShouldReserveWhenAvailable");

        MddpStock mddpStock = new MddpStock();
        mddpStock.setQuantity(2);
        mddpStock.setStockStatus(StockStatus.AVAILABLE);

        VehicleModel vehicleModel = new VehicleModel();
        vehicleModel.setVehicleModelId(1L);
        vehicleModel.setModelName("Model X");

        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(mddpStockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE))
                .thenReturn(Optional.of(mddpStock));
        when(vehicleModelRepository.findById(1L)).thenReturn(Optional.of(vehicleModel));

        OrderResponse response = vehicleOrderService.checkAndReserveMddpStock(orderRequest);

        logger.info("Order status after reserving MDDP stock: {}", response.getOrderStatus());
        assertEquals(OrderStatus.BLOCKED, response.getOrderStatus());
        assertEquals(1, mddpStock.getQuantity());
        assertEquals(StockStatus.AVAILABLE, mddpStock.getStockStatus());

        ArgumentCaptor<StockDetails> stockCaptor = ArgumentCaptor.forClass(StockDetails.class);
        verify(stockRepository).save(stockCaptor.capture());
        StockDetails savedStock = stockCaptor.getValue();
        assertEquals(variant, savedStock.getVehicleVariant());
        assertEquals(vehicleModel, savedStock.getVehicleModel());
        assertEquals(orderRequest.getColour(), savedStock.getColour());
        assertEquals(orderRequest.getFuelType(), savedStock.getFuelType());
        assertEquals(orderRequest.getTransmissionType(), savedStock.getTransmissionType());
        assertEquals(orderRequest.getVariant(), savedStock.getVariant());
        assertEquals(orderRequest.getQuantity(), savedStock.getQuantity());
        assertEquals(StockStatus.AVAILABLE, savedStock.getStockStatus());

        verify(mddpStockRepository).save(mddpStock);
    }

    @Test
    void checkAndBlockStock_ShouldMarkStockDepletedWhenQuantityReachesZero() {
        logger.info("Starting test: checkAndBlockStock_ShouldMarkStockDepletedWhenQuantityReachesZero");
        stock.setQuantity(1);
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(stockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE))
                .thenReturn(List.of(stock));

        OrderResponse response = vehicleOrderService.checkAndBlockStock(orderRequest);

        logger.info("Order status after stock depletion: {}", response.getOrderStatus());
        assertEquals(OrderStatus.BLOCKED, response.getOrderStatus());
        assertEquals(0, stock.getQuantity());
        assertEquals(StockStatus.DEPLETED, stock.getStockStatus());
        verify(stockRepository).save(stock);
    }

    @Test
    void cancelOrder_ShouldThrowWhenOrderAlreadyCompleted() {
        logger.info("Starting test: cancelOrder_ShouldThrowWhenOrderAlreadyCompleted");
        VehicleOrderDetails order = new VehicleOrderDetails();
        order.setCustomerOrderId(1L);
        order.setOrderStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> vehicleOrderService.cancelOrder(1L));
        logger.info("Successfully verified that cancelOrder throws exception for completed order");
    }

    @Test
    void addVehicleModel_ShouldCreateNewModel() {
        logger.info("Starting test: addVehicleModel_ShouldCreateNewModel");
        VehicleModelRequest request = new VehicleModelRequest();
        request.setModelName("Model X");
        request.setCreatedBy("admin");

        VehicleModel savedModel = new VehicleModel();
        savedModel.setVehicleModelId(1L);
        savedModel.setModelName("Model X");

        when(vehicleModelRepository.findByModelName("Model X")).thenReturn(Optional.empty());
        when(vehicleModelRepository.save(any())).thenReturn(savedModel);

        VehicleModelResponse response = vehicleOrderService.addVehicleModel(request);

        logger.info("Vehicle model created with ID: {}", response.getVehicleModelId());
        assertNotNull(response.getVehicleModelId());
        verify(vehicleModelRepository).save(any(VehicleModel.class));
    }

    @Test
    void createFinanceDetails_ShouldCreateNewFinanceRecord() {
        logger.info("Starting test: createFinanceDetails_ShouldCreateNewFinanceRecord");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));
        when(financeDetailsRepository.findByCustomerOrderId(1L)).thenReturn(null);

        FinanceResponse response = financeServiceInjected.createFinanceDetails(financeRequest);

        logger.info("Finance status after creation: {}", response.getFinanceStatus());
        assertEquals(FinanceStatus.PENDING, response.getFinanceStatus());
        verify(financeDetailsRepository).save(any(FinanceDetails.class));
    }

    @Test
    void approveFinance_ShouldUpdateStatusToApproved() {
        logger.info("Starting test: approveFinance_ShouldUpdateStatusToApproved");

        FinanceDetails financeDetails = new FinanceDetails();
        financeDetails.setFinanceId(1L);
        financeDetails.setCustomerOrderId(1L);
        financeDetails.setFinanceStatus(FinanceStatus.PENDING);
        financeDetails.setCustomerName("John Doe");
        financeDetails.setCreatedAt(LocalDateTime.now());
        financeDetails.setUpdatedAt(LocalDateTime.now());

        when(financeDetailsRepository.findByCustomerOrderId(1L)).thenReturn(financeDetails);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));

        when(financeDetailsRepository.save(any(FinanceDetails.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(VehicleOrderDetails.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(historyService).saveFinanceHistory(any(FinanceDetails.class), eq("admin"));
        doNothing().when(historyService).saveOrderHistory(any(VehicleOrderDetails.class), eq("admin"), eq(OrderStatus.ALLOTTED));

        FinanceResponse response = financeServiceInjected.approveFinance(1L, "admin");

        logger.info("Finance status after approval: {}", response.getFinanceStatus());
        assertEquals(FinanceStatus.APPROVED, response.getFinanceStatus());
        assertEquals(OrderStatus.ALLOTTED, response.getOrderStatus());
        assertNotNull(response.getUpdatedAt());

        ArgumentCaptor<FinanceDetails> financeCaptor = ArgumentCaptor.forClass(FinanceDetails.class);
        verify(financeDetailsRepository).save(financeCaptor.capture());
        FinanceDetails savedFinance = financeCaptor.getValue();
        assertEquals(FinanceStatus.APPROVED, savedFinance.getFinanceStatus());
        assertEquals("admin", savedFinance.getApprovedBy());
        assertNotNull(savedFinance.getUpdatedAt());

        ArgumentCaptor<VehicleOrderDetails> orderCaptor = ArgumentCaptor.forClass(VehicleOrderDetails.class);
        verify(orderRepository).save(orderCaptor.capture());
        VehicleOrderDetails savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.ALLOTTED, savedOrder.getOrderStatus());
        assertNotNull(savedOrder.getUpdatedAt());

        verify(historyService).saveFinanceHistory(financeDetails, "admin");
        verify(historyService).saveOrderHistory(orderDetails, "admin", OrderStatus.ALLOTTED);
    }

    @Test
    void rejectFinance_ShouldUpdateStatusToRejected() {
        logger.info("Starting test: rejectFinance_ShouldUpdateStatusToRejected");
        FinanceDetails financeDetails = new FinanceDetails();
        financeDetails.setFinanceStatus(FinanceStatus.PENDING);

        when(financeDetailsRepository.findByCustomerOrderId(1L)).thenReturn(financeDetails);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));

        FinanceResponse response = financeServiceInjected.rejectFinance(1L, "admin");

        logger.info("Finance status after rejection: {}", response.getFinanceStatus());
        assertEquals(FinanceStatus.REJECTED, response.getFinanceStatus());
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
        verify(financeDetailsRepository).save(financeDetails);
        verify(orderRepository).save(orderDetails);
    }

    @Test
    void initiateDispatch_ShouldCreateDispatchDetails() {
        logger.info("Starting test: initiateDispatch_ShouldCreateDispatchDetails");
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));
        when(dispatchDetailsRepository.findByCustomerOrderId(1L)).thenReturn(null);

        DispatchResponse response = dispatchDeliveryService.initiateDispatch(dispatchRequest);

        logger.info("Dispatch status after initiation: {}", response.getDispatchStatus());
        assertEquals(DispatchStatus.PREPARING, response.getDispatchStatus());
        assertEquals(OrderStatus.DISPATCHED, response.getOrderStatus());
        verify(dispatchDetailsRepository).save(any(DispatchDetails.class));
        verify(orderRepository).save(orderDetails);
    }

    @Test
    void confirmDelivery_ShouldCreateDeliveryDetails() {
        logger.info("Starting test: confirmDelivery_ShouldCreateDeliveryDetails");
        orderDetails.setOrderStatus(OrderStatus.DISPATCHED);
        orderDetails.setCreatedAt(LocalDateTime.now());
        orderDetails.setUpdatedAt(LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));
        when(deliveryDetailsRepository.findByCustomerOrderId(1L)).thenReturn(null);

        when(deliveryDetailsRepository.save(any(DeliveryDetails.class)))
                .thenAnswer(invocation -> {
                    DeliveryDetails dd = invocation.getArgument(0);
                    dd.setDeliveryId(1L);
                    return dd;
                });

        DeliveryResponse response = dispatchDeliveryService.confirmDelivery(deliveryRequest);

        logger.info("Delivery status after confirmation: {}", response.getDeliveryStatus());
        assertNotNull(response);
        assertEquals(DeliveryStatus.DELIVERED, response.getDeliveryStatus());
        assertEquals(OrderStatus.DELIVERED, response.getOrderStatus());
        verify(deliveryDetailsRepository).save(any(DeliveryDetails.class));
        verify(orderRepository).save(argThat(order -> order.getOrderStatus() == OrderStatus.DELIVERED));
        verify(historyService).saveOrderHistory(any(VehicleOrderDetails.class), eq("admin"), eq(OrderStatus.DELIVERED));
        verify(historyService).saveDeliveryHistory(any(DeliveryDetails.class), eq("admin"));
    }

    @Test
    void saveOrderHistory_ShouldCreateHistoryRecord() {
        logger.info("Starting test: saveOrderHistory_ShouldCreateHistoryRecord");
        VehicleOrderDetails order = new VehicleOrderDetails();
        order.setCustomerOrderId(1L);
        order.setOrderStatus(OrderStatus.BLOCKED);
        order.setCustomerName("John Doe");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        when(orderHistoryRepository.save(any(VehicleOrderDetailsHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        historyServiceInjected.saveOrderHistory(order, "admin", OrderStatus.ALLOTTED);

        logger.info("Order history saved for order ID: {}", order.getCustomerOrderId());
        verify(orderHistoryRepository).save(any(VehicleOrderDetailsHistory.class));
    }

    @Test
    void saveFinanceHistory_ShouldCreateHistoryRecord() {
        logger.info("Starting test: saveFinanceHistory_ShouldCreateHistoryRecord");
        FinanceDetails finance = new FinanceDetails();
        finance.setFinanceId(1L);
        finance.setFinanceStatus(FinanceStatus.PENDING);
        finance.setCustomerOrderId(1L);
        finance.setCustomerName("John Doe");
        finance.setCreatedAt(LocalDateTime.now());
        finance.setUpdatedAt(LocalDateTime.now());

        when(financeHistoryRepository.save(any(FinanceDetailsHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        historyServiceInjected.saveFinanceHistory(finance, "admin");

        logger.info("Finance history saved for finance ID: {}", finance.getFinanceId());
        verify(financeHistoryRepository).save(any(FinanceDetailsHistory.class));
    }

//    @Test
//    void placeOrder_ShouldStartWorkflow() throws TimeoutException {
//        logger.info("Starting test: placeOrder_ShouldStartWorkflow");
//        OrderRequest request = orderRequest; // Use initialized request from setUp
//        VehicleOrderDetails order = new VehicleOrderDetails();
//        order.setCustomerOrderId(1L);
//
//        VehicleModel model = new VehicleModel();
//        model.setVehicleModelId(1L);
//
//        VehicleVariant variant = new VehicleVariant();
//        variant.setVehicleVariantId(1L);
//        variant.setPrice(new BigDecimal("50000"));
//
//        when(orderRepository.saveAndFlush(any(VehicleOrderDetails.class))).thenReturn(order);
//        when(vehicleModelRepository.findById(1L)).thenReturn(Optional.of(model));
//        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
//
//        OrderResponse orderResponse = new OrderResponse();
//        orderResponse.setCustomerOrderId(1L);
//        orderResponse.setOrderStatus(OrderStatus.PENDING);
//        orderResponse.setCustomerName("John Doe");
//        orderResponse.setModelName("Model X");
//        orderResponse.setCreatedAt(LocalDateTime.now());
//        when(vehicleOrderService.mapToOrderResponse(request)).thenReturn(orderResponse);
//
//        VehicleOrderWorkflow workflowStub = mock(VehicleOrderWorkflow.class);
//        when(workflowClient.newWorkflowStub(eq(VehicleOrderWorkflow.class), any(WorkflowOptions.class)))
//                .thenReturn(workflowStub);
//
//        WorkflowStub untypedWorkflowStub = mock(WorkflowStub.class);
//        when(workflowClient.newUntypedWorkflowStub("order-1")).thenReturn(untypedWorkflowStub);
//        when(untypedWorkflowStub.getResult(30, TimeUnit.SECONDS, OrderResponse.class))
//                .thenThrow(new RuntimeException("Workflow not completed"));
//
//        ResponseEntity<ApiResponse<OrderResponse>> response = vehicleOrderController.placeOrder(request);
//
//        logger.info("Order placed with status: {}", response.getBody().getData().getOrderStatus());
//        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
//        assertNotNull(response.getBody().getData());
//        assertEquals(OrderStatus.PENDING, response.getBody().getData().getOrderStatus());
//        assertEquals("John Doe", response.getBody().getData().getCustomerName());
//        assertEquals("Model X", response.getBody().getData().getModelName());
//        verify(workflowClient).newWorkflowStub(eq(VehicleOrderWorkflow.class), any(WorkflowOptions.class));
//        verify(workflowClient).newUntypedWorkflowStub("order-1");
//        verify(orderRepository).saveAndFlush(any(VehicleOrderDetails.class));
//        verify(orderRepository).save(any(VehicleOrderDetails.class));
//        verify(vehicleOrderService).mapToOrderResponse(request);
//    }



//
//    @Test
//    void cancelOrder_ShouldStartCancelWorkflow() {
//        logger.info("Starting test: cancelOrder_ShouldStartCancelWorkflow");
//        VehicleOrderDetails order = new VehicleOrderDetails();
//        order.setCustomerOrderId(1L);
//        order.setOrderStatus(OrderStatus.BLOCKED);
//        order.setCustomerName("John Doe");
//        order.setModelName("Model X");
//
//        OrderResponse orderResponse = new OrderResponse();
//        orderResponse.setCustomerOrderId(1L);
//        orderResponse.setOrderStatus(OrderStatus.CANCELED);
//        orderResponse.setCustomerName("John Doe");
//        orderResponse.setModelName("Model X");
//
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//        when(vehicleOrderService.cancelOrder(1L)).thenReturn(orderResponse);
//
//        ResponseEntity<ApiResponse<OrderResponse>> response = vehicleOrderController.cancelOrder(1L);
//
//        logger.info("Order canceled with status: {}", response.getBody().getData().getOrderStatus());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(OrderStatus.CANCELED, response.getBody().getData().getOrderStatus());
//        verify(vehicleOrderService).cancelOrder(1L);
//        verify(orderRepository).findById(1L);
//    }








//    @Test
//    void addVehicleModel_ShouldReturnSuccess() {
//        logger.info("Starting test: addVehicleModel_ShouldReturnSuccess");
//        VehicleModelRequest request = new VehicleModelRequest();
//        request.setModelName("Model X");
//        request.setCreatedBy("admin");
//
//        VehicleModelResponse modelResponse = new VehicleModelResponse("Model X", 1L, null);
//
//        when(vehicleOrderService.addVehicleModel(request)).thenReturn(modelResponse);
//
//        ResponseEntity<ApiResponse<VehicleModelResponse>> response = vehicleOrderController.addModel(request);
//
//        logger.info("Vehicle model added: {}", response.getBody().getData().getModelName());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Model X", response.getBody().getData().getModelName());
//        verify(vehicleOrderService).addVehicleModel(request);
//    }




    @Test
    void initiateFinance_ShouldStartWorkflow() {
        logger.info("Starting test: initiateFinance_ShouldStartWorkflow");
        FinanceRequest request = financeRequest; // Use initialized request from setUp

        VehicleOrderDetails order = new VehicleOrderDetails();
        order.setCustomerOrderId(1L);
        order.setOrderStatus(OrderStatus.BLOCKED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        FinanceResponse financeResponse = new FinanceResponse();
        financeResponse.setFinanceStatus(FinanceStatus.PENDING);
        financeResponse.setOrderStatus(OrderStatus.BLOCKED);
        when(financeService.createFinanceDetails(request)).thenReturn(financeResponse);

        FinanceWorkflow workflowStub = mock(FinanceWorkflow.class);
        when(workflowClient.newWorkflowStub(eq(FinanceWorkflow.class), any(WorkflowOptions.class)))
                .thenReturn(workflowStub);

        ResponseEntity<ApiResponse<FinanceResponse>> response = financeController.initiateFinance(request);

        logger.info("Finance initiated with status: {}", response.getBody().getData().getFinanceStatus());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody().getData());
        assertEquals(FinanceStatus.PENDING, response.getBody().getData().getFinanceStatus());
        assertEquals(OrderStatus.BLOCKED, response.getBody().getData().getOrderStatus());
        verify(workflowClient).newWorkflowStub(eq(FinanceWorkflow.class), any(WorkflowOptions.class));
        verify(financeService).createFinanceDetails(request);
        verify(orderRepository).findById(1L);
    }

    @Test
    void approveFinance_ShouldSignalWorkflow() {
        logger.info("Starting test: approveFinance_ShouldSignalWorkflow");
        ApproveFinanceRequest request = new ApproveFinanceRequest();
        request.setCustomerOrderId(1L);

        when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(mock(WorkflowStub.class));

        ResponseEntity<ApiResponse<FinanceResponse>> response = financeController.approveFinance(request);

        logger.info("Finance approval response status: {}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }



//    @Test
//    void initiateDispatch_ShouldStartWorkflow() {
//        logger.info("Starting test: initiateDispatch_ShouldStartWorkflow");
//        DispatchRequest request = dispatchRequest;
//
//        // Mock the service and repository
//        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
//        orderDetails.setCustomerOrderId(1L);
//        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
//
//        DispatchResponse dispatchResponse = new DispatchResponse();
//        dispatchResponse.setDispatchStatus(DispatchStatus.PREPARING);
//        dispatchResponse.setOrderStatus(OrderStatus.DISPATCHED);
//
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderDetails));
//        when(dispatchDeliveryService.initiateDispatch(request)).thenReturn(dispatchResponse);
//
//        DispatchDeliveryWorkflow workflowStub = mock(DispatchDeliveryWorkflow.class);
//        when(workflowClient.newWorkflowStub(eq(DispatchDeliveryWorkflow.class), any(WorkflowOptions.class)))
//                .thenReturn(workflowStub);
//
//        ApiResponse<DispatchResponse> response = dispatchDeliveryController.initiateDispatch(request);
//
//        logger.info("Dispatch initiated with status: {}", response.getData().getDispatchStatus());
//        assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCode());
//        assertNotNull(response.getData());
//        assertEquals(DispatchStatus.PREPARING, response.getData().getDispatchStatus());
//        assertEquals(OrderStatus.DISPATCHED, response.getData().getOrderStatus());
//        verify(workflowClient).newWorkflowStub(eq(DispatchDeliveryWorkflow.class), any(WorkflowOptions.class));
//        verify(dispatchDeliveryService).initiateDispatch(request);
//    }









//
//    @Test
//    void confirmDelivery_ShouldSignalWorkflow() {
//        logger.info("Starting test: confirmDelivery_ShouldSignalWorkflow");
//        DeliveryRequest request = deliveryRequest; // Use initialized request from setUp
//
//        VehicleOrderDetails order = new VehicleOrderDetails();
//        order.setCustomerOrderId(1L);
//        order.setOrderStatus(OrderStatus.DISPATCHED);
//
//        DeliveryResponse deliveryResponse = new DeliveryResponse();
//        deliveryResponse.setDeliveryStatus(DeliveryStatus.DELIVERED);
//        deliveryResponse.setOrderStatus(OrderStatus.DELIVERED);
//
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
//        when(dispatchDeliveryService.confirmDelivery(request)).thenReturn(deliveryResponse);
//        when(workflowClient.newUntypedWorkflowStub("dispatch-delivery-1")).thenReturn(mock(WorkflowStub.class));
//
//        ApiResponse<DeliveryResponse> response = dispatchDeliveryController.confirmDelivery(request);
//
//        logger.info("Delivery confirmation response status: {}", response.getStatusCode());
//        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
//        assertNotNull(response.getData());
//        assertEquals(DeliveryStatus.DELIVERED, response.getData().getDeliveryStatus());
//        verify(dispatchDeliveryService).confirmDelivery(request);
//        verify(workflowClient).newUntypedWorkflowStub("dispatch-delivery-1");
//        verify(orderRepository).findById(1L);
//    }




}