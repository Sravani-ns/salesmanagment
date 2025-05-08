package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApproveFinanceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Customer order ID cannot be null")
    private Long customerOrderId;

    @NotNull(message = "Approved by cannot be null")
    @jakarta.validation.constraints.Size(min = 1, max = 100, message = "Approved by must be between 1 and 100 characters")
    private String approvedBy;
}