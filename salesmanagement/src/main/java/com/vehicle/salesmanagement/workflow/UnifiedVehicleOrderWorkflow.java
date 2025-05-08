package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.UnifiedWorkflowResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface UnifiedVehicleOrderWorkflow {

    @WorkflowMethod
    UnifiedWorkflowResponse processOrder(OrderRequest orderRequest);

    @SignalMethod
    void cancelOrder(Long orderId);

    @SignalMethod
    void approveFinance(String approvedBy);

    @SignalMethod
    void rejectFinance(String rejectedBy);

    @SignalMethod
    void confirmDelivery(DeliveryRequest deliveryRequest);
}