package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.FinanceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "finance_details")
public class FinanceDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financeId;

    @Column(nullable = false)
    private Long customerOrderId;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceStatus financeStatus; // PENDING, APPROVED, REJECTED

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String approvedBy;

    private String rejectedBy;

    public void setVehicleModelId(Long vehicleModelId) {
    }

    public void setVehicleVariantId(Long vehicleVariantId) {
    }

    public void setModelName(String modelName) {
    }

    public void setVariant(String variant) {
    }

    public void setCreatedBy(String customerName) {
    }

    public void setUpdatedBy(String customerName) {
    }

}