package com.vehicle.salesmanagement.domain.dto.apirequest;

import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderRequest {
    //private static final long serialVersionUID = 1L;

    private Long vehicleModelId;
    private Long vehicleVariantId;

    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 100, message = "Customer name must be between 1 and 100 characters")
    private String customerName;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be a valid international number")
    @Size(max = 10, message = "Phone number must not exceed 10 characters")
    private String phoneNumber;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Permanent address must not exceed 500 characters")
    private String permanentAddress;

    @Size(max = 500, message = "Current address must not exceed 500 characters")
    private String currentAddress;

    @NotBlank(message = "Aadhar number cannot be blank")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhar number must be a 12-digit number")
    private String aadharNo;

    @NotBlank(message = "PAN number cannot be blank")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number must be in the format ABCDE1234F")
    private String panNo;

    private String modelName;
    private String fuelType;
    private String colour;
    private String transmissionType;
    private String variant;
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal bookingAmount;
    private String paymentMode;
    private String createdBy;
    private String updatedBy;

    private List<VehicleOrderDetails> vehicleDetails;

    public void setCustomerOrderId(Long customerOrderId) {
    }

    public Long getCustomerOrderId() {
        return getCustomerOrderId();
    }
}