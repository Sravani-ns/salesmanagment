package com.vehicle.salesmanagement.domain.dto.apirequest;

import com.vehicle.salesmanagement.enums.StockStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MddpStockAddRequest {

    @NotNull(message = "Model ID is required")
    @Positive(message = "Model ID must be positive")
    private Long modelId;

    @NotNull(message = "Variant ID is required")
    @Positive(message = "Variant ID must be positive")
    private Long variantId;

    @Size(max = 50, message = "Suffix must not exceed 50 characters")
    private String suffix;

    @NotBlank(message = "Fuel type is required")
    @Size(max = 50, message = "Fuel type must not exceed 50 characters")
    private String fuelType;

    @NotBlank(message = "Colour is required")
    @Size(max = 50, message = "Colour must not exceed 50 characters")
    private String colour;

    @Size(max = 50, message = "Engine colour must not exceed 50 characters")
    private String engineColour;

    @NotBlank(message = "Transmission type is required")
    @Size(max = 50, message = "Transmission type must not exceed 50 characters")
    private String transmissionType;

    @Size(max = 50, message = "Grade must not exceed 50 characters")
    private String grade;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Stock status is required")
    private StockStatus stockStatus;

    @NotNull(message = "Expected dispatch date is required")
    private LocalDateTime expectedDispatchDate;

    @NotNull(message = "Expected delivery date is required")
    private LocalDateTime expectedDeliveryDate;

    @Size(max = 50, message = "Interior colour must not exceed 50 characters")
    private String interiorColour;

    @NotBlank(message = "VIN is required")
    @Size(max = 50, message = "VIN must not exceed 50 characters")
    private String vin;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;
}