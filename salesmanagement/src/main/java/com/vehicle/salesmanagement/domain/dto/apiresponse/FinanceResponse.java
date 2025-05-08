package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FinanceResponse implements Serializable {
    private static final long serialVersionUID = 1L;


    private Long financeId;
    private Long customerOrderId;
    private String customerName;
    private FinanceStatus financeStatus; // PENDING, APPROVED, REJECTED
    private OrderStatus orderStatus; // BLOCKED, ALLOTTED, PENDING, etc.
    private String modelName;
    private String variant;
    private String approvedBy;
    private String rejectedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}