package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class VehicleModelRequest {

    @NotBlank(message = "Model name cannot be blank")
    @Size(min = 1, max = 100, message = "Model name must be between 1 and 100 characters")
    private String modelName;

    @NotBlank(message = "Created by cannot be blank")
    @Size(min = 1, max = 100, message = "Created by must be between 1 and 100 characters")
    private String createdBy;

    @NotBlank(message = "Updated by cannot be blank")
    @Size(min = 1, max = 100, message = "Updated by must be between 1 and 100 characters")
    private String updatedBy;
}
