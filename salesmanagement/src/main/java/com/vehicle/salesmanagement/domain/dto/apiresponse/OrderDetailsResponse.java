package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsResponse {

    private String customerName;
    private String phoneNumber;
    private String email;
    private String aadharNo;
    private String panNo;
    private String modelName;
    private String fuelType;
    private String colour;
    private String variant;
    private int quantity;
    private OrderStatus orderStatus;
}