package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.DispatchDeliveryActivities;
import com.vehicle.salesmanagement.activity.FinanceActivities;
import com.vehicle.salesmanagement.activity.VehicleOrderActivities;
import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.UnifiedWorkflowResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.service.RedisService;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class UnifiedVehicleOrderWorkflowImpl implements UnifiedVehicleOrderWorkflow {

    private final VehicleOrderActivities orderActivities;
    private final FinanceActivities financeActivities;
    private final DispatchDeliveryActivities dispatchDeliveryActivities;
    private final RedisService redisService;

    // State variables for the workflow
    private boolean isCanceled = false;
    private boolean isFinanceApproved = false;
    private boolean isFinanceRejected = false;
    private boolean isDeliveryConfirmed = false;
    private String financeApprovedBy;
    private String financeRejectedBy;
    private DeliveryRequest deliveryRequest;
    private OrderResponse orderResponse;
    private FinanceResponse financeResponse;
    private DispatchResponse dispatchResponse;
    private DeliveryResponse deliveryResponse;

    public UnifiedVehicleOrderWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.orderActivities = Workflow.newActivityStub(VehicleOrderActivities.class, options);
        this.financeActivities = Workflow.newActivityStub(FinanceActivities.class, options);
        this.dispatchDeliveryActivities = Workflow.newActivityStub(DispatchDeliveryActivities.class, options);
        this.redisService = Workflow.newActivityStub(RedisService.class);
    }

    @Override
    public UnifiedWorkflowResponse processOrder(OrderRequest orderRequest) {
        log.info("Starting unified workflow for customer: {}", orderRequest.getCustomerName());
        Long customerOrderId = orderRequest.getCustomerOrderId();

        // Check Redis for existing state
        UnifiedWorkflowResponse cachedState = redisService.getWorkflowState(customerOrderId);
        if (cachedState != null) {
            log.info("Returning cached workflow state for order ID: {}", customerOrderId);
            return cachedState;
        }

        try {
            // Step 1: Process the order (stock check, block, etc.)
            orderResponse = orderActivities.checkStockAvailability(orderRequest);
            orderResponse.setCustomerOrderId(customerOrderId);

            // Check Redis for cancellation signal
            Boolean cancelSignal = (Boolean) redisService.getSignal("cancel", customerOrderId);
            if (cancelSignal != null && cancelSignal) {
                isCanceled = true;
            }

            if (isCanceled) {
                log.info("Order canceled during workflow for customer: {}", orderRequest.getCustomerName());
                orderResponse = orderActivities.cancelOrder(customerOrderId);
                UnifiedWorkflowResponse response = new UnifiedWorkflowResponse(orderResponse, null, null, null);
                redisService.cacheWorkflowState(customerOrderId, response);
                return response;
            }

            if (orderResponse.getOrderStatus() == OrderStatus.PENDING) {
                log.info("Stock not available, manufacturer order placed for: {}", orderRequest.getCustomerName());
                Workflow.await(Duration.ofHours(24), () -> {
                    Boolean cancel = (Boolean) redisService.getSignal("cancel", customerOrderId);
                    return cancel != null && cancel;
                });
                if (isCanceled) {
                    orderResponse = orderActivities.cancelOrder(customerOrderId);
                    UnifiedWorkflowResponse response = new UnifiedWorkflowResponse(orderResponse, null, null, null);
                    redisService.cacheWorkflowState(customerOrderId, response);
                    return response;
                }
                orderResponse.setOrderStatus(OrderStatus.PENDING);
                UnifiedWorkflowResponse response = new UnifiedWorkflowResponse(orderResponse, null, null, null);
                redisService.cacheWorkflowState(customerOrderId, response);
                return response;
            }

            if (orderResponse.getOrderStatus() != OrderStatus.BLOCKED) {
                log.warn("Order did not reach BLOCKED status for customer: {}", orderRequest.getCustomerName());
                UnifiedWorkflowResponse response = new UnifiedWorkflowResponse(orderResponse, null, null, null);
                redisService.cacheWorkflowState(customerOrderId, response);
                return response;
            }

            log.info("Stock blocked for customer: {}", orderRequest.getCustomerName());

            // Step 2: Initiate finance process in parallel if order is BLOCKED
            Promise<FinanceResponse> financePromise = Async.function(() -> {
                if (orderResponse.getOrderStatus() == OrderStatus.BLOCKED) {
                    FinanceRequest financeRequest = new FinanceRequest();
                    financeRequest.setCustomerOrderId(customerOrderId);
                    financeRequest.setCustomerName(orderRequest.getCustomerName());
                    try {
                        financeResponse = financeActivities.createFinanceDetails(financeRequest);
                        // Cache intermediate state
                        redisService.cacheWorkflowState(customerOrderId, new UnifiedWorkflowResponse(orderResponse, financeResponse, null, null));

                        // Check Redis for finance signals
                        Workflow.await(Duration.ofDays(7), () -> {
                            Boolean approved = (Boolean) redisService.getSignal("finance_approve", customerOrderId);
                            Boolean rejected = (Boolean) redisService.getSignal("finance_reject", customerOrderId);
                            if (approved != null && approved) {
                                isFinanceApproved = true;
                                financeApprovedBy = (String) redisService.getSignal("finance_approved_by", customerOrderId);
                            }
                            if (rejected != null && rejected) {
                                isFinanceRejected = true;
                                financeRejectedBy = (String) redisService.getSignal("finance_rejected_by", customerOrderId);
                            }
                            return isFinanceApproved || isFinanceRejected;
                        });

                        if (isFinanceApproved) {
                            financeResponse = financeActivities.approveFinance(customerOrderId, financeApprovedBy);
                            financeResponse.setOrderStatus(OrderStatus.ALLOTTED);
                        } else if (isFinanceRejected) {
                            financeResponse = financeActivities.rejectFinance(customerOrderId, financeRejectedBy);
                            financeResponse.setOrderStatus(OrderStatus.PENDING);
                        } else {
                            financeResponse = financeActivities.rejectFinance(customerOrderId, "SYSTEM_TIMEOUT");
                            financeResponse.setFinanceStatus(FinanceStatus.REJECTED);
                            financeResponse.setOrderStatus(OrderStatus.PENDING);
                        }
                        return financeResponse;
                    } catch (Exception e) {
                        log.error("Finance process failed for order ID: {}: {}", customerOrderId, e.getMessage());
                        throw new RuntimeException("Finance process failed: " + e.getMessage());
                    }
                }
                return null;
            });

            // Step 3: Initiate dispatch/delivery process if finance is approved
            Promise<DeliveryResponse> dispatchDeliveryPromise = Async.function(() -> {
                if (financeResponse != null && financeResponse.getOrderStatus() == OrderStatus.ALLOTTED) {
                    DispatchRequest dispatchRequest = new DispatchRequest();
                    dispatchRequest.setCustomerOrderId(customerOrderId);
                    dispatchRequest.setDispatchedBy("system");

                    Optional<VehicleOrderDetails> orderDetailsOpt = dispatchDeliveryActivities.getVehicleOrderDetails(customerOrderId);
                    if (orderDetailsOpt.isPresent()) {
                        VehicleOrderDetails orderDetails = orderDetailsOpt.get();
                        OrderStatus currentStatus = orderDetails.getOrderStatus();
                        if (currentStatus == OrderStatus.ALLOTTED) {
                            dispatchResponse = dispatchDeliveryActivities.initiateDispatch(dispatchRequest);
                            if (dispatchResponse.getOrderStatus() != OrderStatus.DISPATCHED) {
                                throw new RuntimeException("Dispatch failed for order ID: " + customerOrderId);
                            }
                            // Cache intermediate state
                            redisService.cacheWorkflowState(customerOrderId, new UnifiedWorkflowResponse(orderResponse, financeResponse, dispatchResponse, null));

                            // Check Redis for delivery confirmation signal
                            Workflow.await(Duration.ofMinutes(1), () -> {
                                DeliveryRequest cachedDeliveryRequest = (DeliveryRequest) redisService.getSignal("delivery_confirm", customerOrderId);
                                if (cachedDeliveryRequest != null) {
                                    deliveryRequest = cachedDeliveryRequest;
                                    isDeliveryConfirmed = true;
                                }
                                return isDeliveryConfirmed;
                            });

                            if (!isDeliveryConfirmed || deliveryRequest == null) {
                                throw new RuntimeException("Delivery not confirmed for order ID: " + customerOrderId);
                            }

                            // Confirm delivery
                            orderDetailsOpt = dispatchDeliveryActivities.getVehicleOrderDetails(customerOrderId);
                            if (orderDetailsOpt.isPresent()) {
                                currentStatus = orderDetailsOpt.get().getOrderStatus();
                                if (currentStatus == OrderStatus.DISPATCHED) {
                                    deliveryResponse = dispatchDeliveryActivities.confirmDelivery(deliveryRequest);
                                    if (deliveryResponse.getOrderStatus() != OrderStatus.DELIVERED) {
                                        throw new RuntimeException("Delivery confirmation failed for order ID: " + customerOrderId);
                                    }
                                }
                            }
                            return deliveryResponse;
                        }
                    }
                }
                return null;
            });

            // Wait for both finance and dispatch/delivery processes to complete
            financeResponse = financePromise.get();
            deliveryResponse = dispatchDeliveryPromise.get();

            UnifiedWorkflowResponse finalResponse = new UnifiedWorkflowResponse(orderResponse, financeResponse, dispatchResponse, deliveryResponse);
            redisService.cacheWorkflowState(customerOrderId, finalResponse);

            log.info("Unified workflow completed for customer: {}", orderRequest.getCustomerName());
            return finalResponse;

        } catch (Exception e) {
            log.error("Unified workflow failed for customer {}: {}", orderRequest.getCustomerName(), e.getMessage(), e);
            if (orderResponse == null) {
                orderResponse = mapToOrderResponse(orderRequest);
                orderResponse.setOrderStatus(OrderStatus.PENDING);
            }
            UnifiedWorkflowResponse errorResponse = new UnifiedWorkflowResponse(orderResponse, financeResponse, dispatchResponse, deliveryResponse);
            redisService.cacheWorkflowState(customerOrderId, errorResponse);
            return errorResponse;
        }
    }

    @Override
    public void cancelOrder(Long orderId) {
        log.info("Received cancel signal for order ID: {}", orderId);
        redisService.cacheSignal("cancel", orderId, true);
        isCanceled = true;
    }

    @Override
    public void approveFinance(String approvedBy) {
        Long customerOrderId = Long.valueOf(Workflow.getInfo().getWorkflowId().split("-")[1]);
        log.info("Received finance approval signal for approvedBy: {}", approvedBy);
        redisService.cacheSignal("finance_approve", customerOrderId, true);
        redisService.cacheSignal("finance_approved_by", customerOrderId, approvedBy);
        this.financeApprovedBy = approvedBy;
        this.isFinanceApproved = true;
    }

    @Override
    public void rejectFinance(String rejectedBy) {
        Long customerOrderId = Long.valueOf(Workflow.getInfo().getWorkflowId().split("-")[1]);
        log.info("Received finance rejection signal for rejectedBy: {}", rejectedBy);
        redisService.cacheSignal("finance_reject", customerOrderId, true);
        redisService.cacheSignal("finance_rejected_by", customerOrderId, rejectedBy);
        this.financeRejectedBy = rejectedBy;
        this.isFinanceRejected = true;
    }

    @Override
    public void confirmDelivery(DeliveryRequest deliveryRequest) {
        log.info("Received delivery confirmation signal for order ID: {}", deliveryRequest.getCustomerOrderId());
        redisService.cacheSignal("delivery_confirm", deliveryRequest.getCustomerOrderId(), deliveryRequest);
        this.deliveryRequest = deliveryRequest;
        this.isDeliveryConfirmed = true;
    }

    private OrderResponse mapToOrderResponse(OrderRequest request) {
        OrderResponse response = new OrderResponse();
        response.setCustomerOrderId(request.getCustomerOrderId());
        response.setVehicleModelId(request.getVehicleModelId());
        response.setVehicleVariantId(request.getVehicleVariantId());
        response.setCustomerName(request.getCustomerName());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setEmail(request.getEmail());
        response.setPermanentAddress(request.getPermanentAddress());
        response.setCurrentAddress(request.getCurrentAddress());
        response.setAadharNo(request.getAadharNo());
        response.setPanNo(request.getPanNo());
        response.setModelName(request.getModelName());
        response.setFuelType(request.getFuelType());
        response.setColour(request.getColour());
        response.setTransmissionType(request.getTransmissionType());
        response.setVariant(request.getVariant());
        response.setQuantity(request.getQuantity());
        response.setTotalPrice(request.getTotalPrice());
        response.setBookingAmount(request.getBookingAmount());
        response.setPaymentMode(request.getPaymentMode());
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}