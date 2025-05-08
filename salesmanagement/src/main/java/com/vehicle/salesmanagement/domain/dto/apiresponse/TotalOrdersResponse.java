package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotalOrdersResponse {
    private Long totalOrders;

    public TotalOrdersResponse(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
}