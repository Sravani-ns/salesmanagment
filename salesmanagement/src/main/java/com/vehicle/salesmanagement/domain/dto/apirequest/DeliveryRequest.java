package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeliveryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Customer order ID cannot be null")
    private Long customerOrderId;

    @NotNull(message = "Delivered by cannot be null")
    @Size(min = 1, max = 100, message = "Delivered by must be between 1 and 100 characters")
    private String deliveredBy;

    @NotNull(message = "Recipient name cannot be null")
    @Size(min = 1, max = 100, message = "Recipient name must be between 1 and 100 characters")
    private String recipientName;
    private String createdBy;
    private String updatedBy;
}