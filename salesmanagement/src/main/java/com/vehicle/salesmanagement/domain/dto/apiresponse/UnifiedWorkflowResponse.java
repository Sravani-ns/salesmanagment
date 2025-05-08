package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class UnifiedWorkflowResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private OrderResponse orderResponse;
    private FinanceResponse financeResponse;
    private DispatchResponse dispatchResponse;
    private DeliveryResponse deliveryResponse;

    public UnifiedWorkflowResponse(OrderResponse orderResponse, FinanceResponse financeResponse,
                                  DispatchResponse dispatchResponse, DeliveryResponse deliveryResponse) {
        this.orderResponse = orderResponse;
        this.financeResponse = financeResponse;
        this.dispatchResponse = dispatchResponse;
        this.deliveryResponse = deliveryResponse;
    }

    public OrderStatus getOverallOrderStatus() {
        if (deliveryResponse != null && deliveryResponse.getOrderStatus() == OrderStatus.DELIVERED) {
            return OrderStatus.DELIVERED;
        } else if (dispatchResponse != null && dispatchResponse.getOrderStatus() == OrderStatus.DISPATCHED) {
            return OrderStatus.DISPATCHED;
        } else if (financeResponse != null && financeResponse.getOrderStatus() == OrderStatus.ALLOTTED) {
            return OrderStatus.ALLOTTED;
        } else if (orderResponse != null && orderResponse.getOrderStatus() != null) {
            return orderResponse.getOrderStatus();
        } else {
            return OrderStatus.PENDING;
        }
    }

    public String getOverallStatusMessage() {
        StringBuilder status = new StringBuilder();
        status.append("Order: ").append(orderResponse != null && orderResponse.getOrderStatus() != null ?
                orderResponse.getOrderStatus() : "Pending");
        status.append(", Finance: ").append(financeResponse != null && financeResponse.getFinanceStatus() != null ?
                financeResponse.getFinanceStatus() : "Pending");
        status.append(", Dispatch: ").append(dispatchResponse != null && dispatchResponse.getDispatchStatus() != null ?
                dispatchResponse.getDispatchStatus() : "Pending");
        status.append(", Delivery: ").append(deliveryResponse != null && deliveryResponse.getDeliveryStatus() != null ?
                deliveryResponse.getDeliveryStatus() : "Pending");
        return status.toString();
    }
}