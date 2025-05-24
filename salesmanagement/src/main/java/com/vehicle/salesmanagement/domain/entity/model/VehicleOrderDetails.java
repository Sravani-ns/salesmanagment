//package com.vehicle.salesmanagement.domain.entity.model;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.vehicle.salesmanagement.enums.OrderStatus;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Data
//@Table(name = "vehicle_order_details", schema = "sales_tracking")
//@AllArgsConstructor
//@NoArgsConstructor
//@JsonIgnoreProperties({"vehicleModel", "vehicleVariant"})
//
//public class VehicleOrderDetails {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "customer_order_id")
//    private Long customerOrderId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "vehicle_model_id", nullable = false)
//    @NotNull(message = "Vehicle model is required")
//    private VehicleModel vehicleModel;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "vehicle_variant_id", nullable = false)
//    @NotNull(message = "Vehicle variant is required")
//    private VehicleVariant vehicleVariant;
//
//    @Column(name = "customer_name", length = 100, nullable = false)
//    @NotBlank(message = "Customer name is required")
//    private String customerName;
//
//    @Column(name = "phone_number", length = 15, nullable = false)
//    @NotBlank(message = "Phone number is required")
//    private String phoneNumber;
//
//    @Column(name = "email", length = 100, nullable = false)
//    @NotBlank(message = "Email is required")
//    private String email;
//
//    @Column(name = "permanent_address", columnDefinition = "TEXT")
//    private String permanentAddress;
//
//    @Column(name = "current_address", columnDefinition = "TEXT")
//    private String currentAddress;
//
//    @Column(name = "aadhar_no", length = 20, nullable = false)
//    @NotBlank(message = "Aadhar number is required")
//    private String aadharNo;
//
//    @Column(name = "pan_no", length = 20, nullable = false)
//    @NotBlank(message = "PAN number is required")
//    private String panNo;
//
//    @Column(name = "model_name", length = 100, nullable = false)
//    @NotBlank(message = "Model name is required")
//    private String modelName;
//
//    @Column(name = "fuel_type", length = 50)
//    private String fuelType;
//
//    @Column(name = "colour", length = 50)
//    private String colour;
//
//    @Column(name = "transmission_type", length = 50)
//    private String transmissionType;
//
//    @Column(name = "variant", length = 50)
//    private String variant;
//
//    @Column(name = "quantity", nullable = false)
//    @NotNull(message = "Quantity is required")
//    private Integer quantity;
//
//    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
//    @NotNull(message = "Total price is required")
//    private BigDecimal totalPrice;
//
//    @Column(name = "booking_amount", precision = 15, scale = 2, nullable = false)
//    @NotNull(message = "Booking amount is required")
//    private BigDecimal bookingAmount;
//
//    @Column(name = "payment_mode", length = 50, nullable = false)
//    @NotBlank(message = "Payment mode is required")
//    private String paymentMode;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "order_status")
//    private OrderStatus orderStatus;
//
//    @Column(name = "created_at", nullable = false)
//    @NotNull(message = "Created at is required")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    @NotNull(message = "Updated at is required")
//    private LocalDateTime updatedAt;
//
//    @Column(name = "created_by", length = 100, nullable = false)
//    @NotBlank(message = "Created by is required")
//    private String createdBy;
//
//    @Column(name = "updated_by", length = 100, nullable = false)
//    @NotBlank(message = "Updated by is required")
//    private String updatedBy;
//
//
//    public Long getCustomerOrderId() {
//        return customerOrderId;
//    }
//
//    public void setCustomerOrderId(Long customerOrderId) {
//        this.customerOrderId = customerOrderId;
//    }
//
//    public @NotNull(message = "Vehicle model is required") VehicleModel getVehicleModel() {
//        return vehicleModel;
//    }
//
//    public void setVehicleModel(@NotNull(message = "Vehicle model is required") VehicleModel vehicleModel) {
//        this.vehicleModel = vehicleModel;
//    }
//
//    public @NotNull(message = "Vehicle variant is required") VehicleVariant getVehicleVariant() {
//        return vehicleVariant;
//    }
//
//    public void setVehicleVariant(@NotNull(message = "Vehicle variant is required") VehicleVariant vehicleVariant) {
//        this.vehicleVariant = vehicleVariant;
//    }
//
//    public @NotBlank(message = "Customer name is required") String getCustomerName() {
//        return customerName;
//    }
//
//    public void setCustomerName(@NotBlank(message = "Customer name is required") String customerName) {
//        this.customerName = customerName;
//    }
//
//    public @NotBlank(message = "Phone number is required") String getPhoneNumber() {
//        return phoneNumber;
//    }
//
//    public void setPhoneNumber(@NotBlank(message = "Phone number is required") String phoneNumber) {
//        this.phoneNumber = phoneNumber;
//    }
//
//    public @NotBlank(message = "Email is required") String getEmail() {
//        return email;
//    }
//
//    public void setEmail(@NotBlank(message = "Email is required") String email) {
//        this.email = email;
//    }
//
//    public String getPermanentAddress() {
//        return permanentAddress;
//    }
//
//    public void setPermanentAddress(String permanentAddress) {
//        this.permanentAddress = permanentAddress;
//    }
//
//    public String getCurrentAddress() {
//        return currentAddress;
//    }
//
//    public void setCurrentAddress(String currentAddress) {
//        this.currentAddress = currentAddress;
//    }
//
//    public @NotBlank(message = "Aadhar number is required") String getAadharNo() {
//        return aadharNo;
//    }
//
//    public void setAadharNo(@NotBlank(message = "Aadhar number is required") String aadharNo) {
//        this.aadharNo = aadharNo;
//    }
//
//    public @NotBlank(message = "PAN number is required") String getPanNo() {
//        return panNo;
//    }
//
//    public void setPanNo(@NotBlank(message = "PAN number is required") String panNo) {
//        this.panNo = panNo;
//    }
//
//    public @NotBlank(message = "Model name is required") String getModelName() {
//        return modelName;
//    }
//
//    public void setModelName(@NotBlank(message = "Model name is required") String modelName) {
//        this.modelName = modelName;
//    }
//
//    public String getFuelType() {
//        return fuelType;
//    }
//
//    public void setFuelType(String fuelType) {
//        this.fuelType = fuelType;
//    }
//
//    public String getColour() {
//        return colour;
//    }
//
//    public void setColour(String colour) {
//        this.colour = colour;
//    }
//
//    public String getTransmissionType() {
//        return transmissionType;
//    }
//
//    public void setTransmissionType(String transmissionType) {
//        this.transmissionType = transmissionType;
//    }
//
//    public String getVariant() {
//        return variant;
//    }
//
//    public void setVariant(String variant) {
//        this.variant = variant;
//    }
//
//    public @NotNull(message = "Quantity is required") Integer getQuantity() {
//        return quantity;
//    }
//
//    public void setQuantity(@NotNull(message = "Quantity is required") Integer quantity) {
//        this.quantity = quantity;
//    }
//
//    public @NotNull(message = "Total price is required") BigDecimal getTotalPrice() {
//        return totalPrice;
//    }
//
//    public void setTotalPrice(@NotNull(message = "Total price is required") BigDecimal totalPrice) {
//        this.totalPrice = totalPrice;
//    }
//
//    public @NotNull(message = "Booking amount is required") BigDecimal getBookingAmount() {
//        return bookingAmount;
//    }
//
//    public void setBookingAmount(@NotNull(message = "Booking amount is required") BigDecimal bookingAmount) {
//        this.bookingAmount = bookingAmount;
//    }
//
//    public @NotBlank(message = "Payment mode is required") String getPaymentMode() {
//        return paymentMode;
//    }
//
//    public void setPaymentMode(@NotBlank(message = "Payment mode is required") String paymentMode) {
//        this.paymentMode = paymentMode;
//    }
//
//    public OrderStatus getOrderStatus() {
//        return orderStatus;
//    }
//
//    public void setOrderStatus(OrderStatus orderStatus) {
//        this.orderStatus = orderStatus;
//    }
//
//    public @NotNull(message = "Created at is required") LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(@NotNull(message = "Created at is required") LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public @NotNull(message = "Updated at is required") LocalDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setUpdatedAt(@NotNull(message = "Updated at is required") LocalDateTime updatedAt) {
//        this.updatedAt = updatedAt;
//    }
//
//    public @NotBlank(message = "Created by is required") String getCreatedBy() {
//        return createdBy;
//    }
//
//    public void setCreatedBy(@NotBlank(message = "Created by is required") String createdBy) {
//        this.createdBy = createdBy;
//    }
//
//    public @NotBlank(message = "Updated by is required") String getUpdatedBy() {
//        return updatedBy;
//    }
//
//    public void setUpdatedBy(@NotBlank(message = "Updated by is required") String updatedBy) {
//        this.updatedBy = updatedBy;
//    }
//
//
//    public String getVehicleModelId() {
//        return getVehicleModelId();
//    }
//
//
//    public String getVehicleVariantId() {
//        return getVehicleVariantId();
//    }
//
//    public void setVehicleModelId(Long vehicleModelId) {
//    }
//
//    public void setVehicleVariantId(Long vehicleVariantId) {
//
//    }
//
//    public String getWorkflowId() {
//        return getWorkflowId();
//    }
//
//    public void setWorkflowId(String workflowId) {
//    }
//}

package com.vehicle.salesmanagement.domain.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vehicle.salesmanagement.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle_order_details", schema = "sales_tracking")
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"vehicleModel", "vehicleVariant"})
public class VehicleOrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_order_id")
    private Long customerOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    @NotNull(message = "Vehicle model is required")
    private VehicleModel vehicleModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_variant_id", nullable = false)
    @NotNull(message = "Vehicle variant is required")
    private VehicleVariant vehicleVariant;

    @Column(name = "customer_name", length = 100, nullable = false)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Column(name = "phone_number", length = 15, nullable = false)
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Column(name = "email", length = 100, nullable = false)
    @NotBlank(message = "Email is required")
    private String email;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;

    @Column(name = "aadhar_no", length = 20, nullable = false)
    @NotBlank(message = "Aadhar number is required")
    private String aadharNo;

    @Column(name = "pan_no", length = 20, nullable = false)
    @NotBlank(message = "PAN number is required")
    private String panNo;

    @Column(name = "model_name", length = 100, nullable = false)
    @NotBlank(message = "Model name is required")
    private String modelName;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "colour", length = 50)
    private String colour;

    @Column(name = "transmission_type", length = 50)
    private String transmissionType;

    @Column(name = "variant", length = 50)
    private String variant;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    @NotNull(message = "Total price is required")
    private BigDecimal totalPrice;

    @Column(name = "booking_amount", precision = 15, scale = 2, nullable = false)
    @NotNull(message = "Booking amount is required")
    private BigDecimal bookingAmount;

    @Column(name = "payment_mode", length = 50, nullable = false)
    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100, nullable = false)
    @NotBlank(message = "Created by is required")
    private String createdBy;

    @Column(name = "updated_by", length = 100, nullable = false)
    @NotBlank(message = "Updated by is required")
    private String updatedBy;

    public Long getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(Long customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public @NotNull(message = "Vehicle model is required") VehicleModel getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(VehicleModel vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public @NotNull(message = "Vehicle variant is required") VehicleVariant getVehicleVariant() {
        return vehicleVariant;
    }

    public void setVehicleVariant(@NotNull(message = "Vehicle variant is required") VehicleVariant vehicleVariant) {
        this.vehicleVariant = vehicleVariant;
    }

    public @NotBlank(message = "Customer name is required") String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(@NotBlank(message = "Customer name is required") String customerName) {
        this.customerName = customerName;
    }

    public @NotBlank(message = "Phone number is required") String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NotBlank(message = "Phone number is required") String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public @NotBlank(message = "Email is required") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") String email) {
        this.email = email;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public @NotBlank(message = "Aadhar number is required") String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(@NotBlank(message = "Aadhar number is required") String aadharNo) {
        this.aadharNo = aadharNo;
    }

    public @NotBlank(message = "PAN number is required") String getPanNo() {
        return panNo;
    }

    public void setPanNo(@NotBlank(message = "PAN number is required") String panNo) {
        this.panNo = panNo;
    }

    public @NotBlank(message = "Model name is required") String getModelName() {
        return modelName;
    }

    public void setModelName(@NotBlank(message = "Model name is required") String modelName) {
        this.modelName = modelName;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getTransmissionType() {
        return transmissionType;
    }

    public void setTransmissionType(String transmissionType) {
        this.transmissionType = transmissionType;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public @NotNull(message = "Quantity is required") Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(@NotNull(message = "Quantity is required") Integer quantity) {
        this.quantity = quantity;
    }

    public @NotNull(message = "Total price is required") BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(@NotNull(message = "Total price is required") BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public @NotNull(message = "Booking amount is required") BigDecimal getBookingAmount() {
        return bookingAmount;
    }

    public void setBookingAmount(@NotNull(message = "Booking amount is required") BigDecimal bookingAmount) {
        this.bookingAmount = bookingAmount;
    }

    public @NotBlank(message = "Payment mode is required") String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(@NotBlank(message = "Payment mode is required") String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public @NotNull(message = "Created at is required") LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull(message = "Created at is required") LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull(message = "Updated at is required") LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NotNull(message = "Updated at is required") LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @NotBlank(message = "Created by is required") String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(@NotBlank(message = "Created by is required") String createdBy) {
        this.createdBy = createdBy;
    }

    public @NotBlank(message = "Updated by is required") String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(@NotBlank(message = "Updated by is required") String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVehicleModelId() {
        return vehicleModel != null ? vehicleModel.getVehicleModelId() : null;
    }

    public Long getVehicleVariantId() {
        return vehicleVariant != null ? vehicleVariant.getVehicleVariantId() : null;
    }

    public void setVehicleModelId(Long vehicleModelId) {
        if (vehicleModelId != null) {
            VehicleModel model = new VehicleModel();
            model.setVehicleModelId(vehicleModelId);
            this.vehicleModel = model;
        }
    }

    public void setVehicleVariantId(Long vehicleVariantId) {
        if (vehicleVariantId != null) {
            VehicleVariant variant = new VehicleVariant();
            variant.setVehicleVariantId(vehicleVariantId);
            this.vehicleVariant = variant;
        }
    }

    public String getWorkflowId() {
        return null; // Workflow ID is managed externally
    }

    public void setWorkflowId(String workflowId) {
        // No-op: Workflow ID is managed externally
    }


}