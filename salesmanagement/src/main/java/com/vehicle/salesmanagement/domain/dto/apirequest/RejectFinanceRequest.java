package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class RejectFinanceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Customer order ID cannot be null")
    private Long customerOrderId;

    @NotNull(message = "Rejected by cannot be null")
    @Size(min = 1, max = 100, message = "Rejected by must be between 1 and 100 characters")
    private String rejectedBy;
}
