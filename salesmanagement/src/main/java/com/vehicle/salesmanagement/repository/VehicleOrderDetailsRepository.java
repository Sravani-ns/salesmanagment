package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface VehicleOrderDetailsRepository extends JpaRepository<VehicleOrderDetails,Long> {

    @Query("SELECT COUNT(o) FROM VehicleOrderDetails o")
    Long countTotalOrders();

    @Query("SELECT COUNT(o) FROM VehicleOrderDetails o WHERE o.orderStatus = 'PENDING'")
    Long countPendingOrders();

    @Query("SELECT COUNT(o) FROM VehicleOrderDetails o WHERE o.orderStatus = 'PENDING_FINANCE'")
    Long countFinancePendingOrders();

    @Query("SELECT COUNT(o) FROM VehicleOrderDetails o WHERE o.orderStatus = 'DELIVERED'")
    Long countClosedOrders();


}
