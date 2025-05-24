package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.MddpStock;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MddpStockRepository extends JpaRepository<MddpStock, Long> {
    @Query("SELECT m FROM MddpStock m WHERE m.vehicleVariant = :vehicleVariant AND m.stockStatus = :stockStatus")
    Optional<MddpStock> findByVehicleVariantAndStockStatus(@Param("vehicleVariant") VehicleVariant vehicleVariant, @Param("stockStatus") StockStatus stockStatus);

    List<MddpStock> findByStockStatus(StockStatus stockStatus);
}