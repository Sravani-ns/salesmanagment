package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VehicleVariantRepository extends JpaRepository<VehicleVariant,Long> {



    @Query("SELECT DISTINCT v.vehicleModel.modelName FROM VehicleVariant v")
    List<String> findDistinctModelNames();

    @Query("SELECT DISTINCT v.variant FROM VehicleVariant v")
    List<String> findDistinctVariants();

    @Query("SELECT DISTINCT v.colour FROM VehicleVariant v")
    List<String> findDistinctColours();

    @Query("SELECT DISTINCT v.engineCapacity FROM VehicleVariant v")
    List<String> findDistinctEngineCapacities();

    @Query("SELECT DISTINCT v.fuelType FROM VehicleVariant v")
    List<String> findDistinctFuelTypes();

    @Query("SELECT DISTINCT v.transmissionType FROM VehicleVariant v")
    List<String> findDistinctTransmissionTypes();

    @Query("SELECT DISTINCT v.price FROM VehicleVariant v")
    List<BigDecimal> findDistinctPrices();

    @Query("SELECT DISTINCT v.yearOfManufacture FROM VehicleVariant v")
    List<Integer> findDistinctYearsOfManufacture();

    @Query("SELECT DISTINCT v.bodyType FROM VehicleVariant v")
    List<String> findDistinctBodyTypes();

    @Query("SELECT DISTINCT v.fuelTankCapacity FROM VehicleVariant v")
    List<String> findDistinctFuelTankCapacities();

    @Query("SELECT DISTINCT v.numberOfAirBags FROM VehicleVariant v")
    List<Integer> findDistinctNumberOfAirbags();

    @Query("SELECT DISTINCT v.mileageCity FROM VehicleVariant v")
    List<String> findDistinctMileageCities();

    @Query("SELECT DISTINCT v.mileageHighway FROM VehicleVariant v")
    List<String> findDistinctMileageHighways();

    @Query("SELECT DISTINCT v.seatingCapacity FROM VehicleVariant v")
    List<String> findDistinctSeatingCapacities();

    @Query("SELECT DISTINCT v.maxPower FROM VehicleVariant v")
    List<String> findDistinctMaxPowers();

    @Query("SELECT DISTINCT v.maxTorque FROM VehicleVariant v")
    List<String> findDistinctMaxTorques();

    @Query("SELECT DISTINCT v.topSpeed FROM VehicleVariant v")
    List<String> findDistinctTopSpeeds();

    @Query("SELECT DISTINCT v.wheelBase FROM VehicleVariant v")
    List<String> findDistinctWheelBases();

    @Query("SELECT DISTINCT v.width FROM VehicleVariant v")
    List<String> findDistinctWidths();

    @Query("SELECT DISTINCT v.length FROM VehicleVariant v")
    List<String> findDistinctLengths();

    @Query("SELECT DISTINCT v.safetyFeature FROM VehicleVariant v")
    List<String> findDistinctSafetyFeatures();

    @Query("SELECT DISTINCT v.infotainment FROM VehicleVariant v")
    List<String> findDistinctInfotainments();

    @Query("SELECT DISTINCT v.comfort FROM VehicleVariant v")
    List<String> findDistinctComforts();

    @Query("SELECT DISTINCT v.suffix FROM VehicleVariant v")
    List<String> findDistinctSuffixes();

    @Query("SELECT DISTINCT v.engineColour FROM VehicleVariant v")
    List<String> findDistinctEngineColours();

    @Query("SELECT DISTINCT v.interiorColour FROM VehicleVariant v")
    List<String> findDistinctInteriorColours();

    @Query("SELECT DISTINCT v.vinNumber FROM VehicleVariant v")
    List<String> findDistinctVinNumbers();

    // New methods to filter by modelName
    @Query("SELECT DISTINCT v.variant FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctVariantsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.colour FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctColoursByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.engineCapacity FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctEngineCapacitiesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.fuelType FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctFuelTypesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.transmissionType FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctTransmissionTypesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.price FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<BigDecimal> findDistinctPricesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.yearOfManufacture FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<Integer> findDistinctYearsOfManufactureByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.bodyType FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctBodyTypesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.fuelTankCapacity FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctFuelTankCapacitiesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.numberOfAirBags FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<Integer> findDistinctNumberOfAirbagsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.mileageCity FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctMileageCitiesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.mileageHighway FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctMileageHighwaysByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.seatingCapacity FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctSeatingCapacitiesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.maxPower FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctMaxPowersByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.maxTorque FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctMaxTorquesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.topSpeed FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctTopSpeedsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.wheelBase FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctWheelBasesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.width FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctWidthsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.length FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctLengthsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.safetyFeature FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctSafetyFeaturesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.infotainment FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctInfotainmentsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.comfort FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctComfortsByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.suffix FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctSuffixesByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.engineColour FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctEngineColoursByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.interiorColour FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctInteriorColoursByModelName(@Param("modelName") String modelName);

    @Query("SELECT DISTINCT v.vinNumber FROM VehicleVariant v WHERE v.vehicleModel.modelName = :modelName")
    List<String> findDistinctVinNumbersByModelName(@Param("modelName") String modelName);

    List<VehicleVariant> findByVehicleModel_ModelNameAndVariant(String modelName, String variant);

    List<VehicleVariant> findByVehicleModel_ModelName(String modelName);
}
