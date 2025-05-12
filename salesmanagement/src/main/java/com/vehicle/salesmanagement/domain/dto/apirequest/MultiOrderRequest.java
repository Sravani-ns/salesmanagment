package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MultiOrderRequest {
    @NotEmpty(message = "Vehicle orders list cannot be empty")
    @Valid
    private List<OrderRequest> vehicleOrders;
}