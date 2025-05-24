package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleAttributesResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.repository.VehicleModelRepository;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DropdownService {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleVariantRepository vehicleVariantRepository;

    public VehicleAttributesResponse getVehicleAttributes(String modelName, String variant) {
        VehicleAttributesResponse response = new VehicleAttributesResponse();
        
        // Fetch all model names, handle nulls
        List<String> modelNames = vehicleModelRepository.findAll().stream()
                .map(model -> model.getModelName() != null ? model.getModelName() : "")
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        response.setModelNames(modelNames);

        // Initialize model details map
        Map<String, VehicleAttributesResponse.ModelAttributes> modelDetails = new HashMap<>();
        response.setModelDetails(modelDetails);

        // Fetch variants based on modelName and variant
        List<VehicleVariant> variants;
        if (modelName != null && variant != null) {
            variants = vehicleVariantRepository.findByVehicleModel_ModelNameAndVariant(modelName, variant);
        } else if (modelName != null) {
            variants = vehicleVariantRepository.findByVehicleModel_ModelName(modelName);
        } else {
            variants = vehicleVariantRepository.findAll();
        }

        // Handle empty variants for specific model or model+variant
        if (variants.isEmpty() && modelName != null) {
            VehicleAttributesResponse.ModelAttributes emptyAttributes = new VehicleAttributesResponse.ModelAttributes();
            if (variant != null) {
                emptyAttributes.setVariants(Collections.singletonList(variant));
            }
            modelDetails.put(modelName, emptyAttributes);
            return response;
        }

        // Group variants by model name, filter out invalid entries
        Map<String, List<VehicleVariant>> variantsByModel = variants.stream()
                .filter(v -> v.getVehicleModel() != null && v.getVehicleModel().getModelName() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicleModel().getModelName()));

        // Populate attributes for each model
        variantsByModel.forEach((mName, modelVariants) -> {
            VehicleAttributesResponse.ModelAttributes attributes = new VehicleAttributesResponse.ModelAttributes();

            attributes.setVariants(modelVariants.stream()
                    .map(VehicleVariant::getVariant)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setColours(modelVariants.stream()
                    .map(VehicleVariant::getColour)
                    .filter(Objects::nonNull)
                    .flatMap(colour -> Arrays.stream(colour.split(",\\s*")))
                    .filter(colour -> !colour.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setEngineColours(modelVariants.stream()
                    .map(VehicleVariant::getEngineColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setInteriorColours(modelVariants.stream()
                    .map(VehicleVariant::getInteriorColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setFuelTypes(modelVariants.stream()
                    .map(VehicleVariant::getFuelType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setTransmissionTypes(modelVariants.stream()
                    .map(VehicleVariant::getTransmissionType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setPrices(modelVariants.stream()
                    .map(VehicleVariant::getPrice)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setYearsOfManufacture(modelVariants.stream()
                    .map(VehicleVariant::getYearOfManufacture)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setBodyTypes(modelVariants.stream()
                    .map(VehicleVariant::getBodyType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setFuelTankCapacities(modelVariants.stream()
                    .map(VehicleVariant::getFuelTankCapacity)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setNumberOfAirbags(modelVariants.stream()
                    .map(VehicleVariant::getNumberOfAirBags)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMileageCities(modelVariants.stream()
                    .map(VehicleVariant::getMileageCity)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMileageHighways(modelVariants.stream()
                    .map(VehicleVariant::getMileageHighway)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSeatingCapacities(modelVariants.stream()
                    .map(VehicleVariant::getSeatingCapacity)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMaxPowers(modelVariants.stream()
                    .map(VehicleVariant::getMaxPower)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMaxTorques(modelVariants.stream()
                    .map(VehicleVariant::getMaxTorque)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setTopSpeeds(modelVariants.stream()
                    .map(VehicleVariant::getTopSpeed)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setWheelBases(modelVariants.stream()
                    .map(VehicleVariant::getWheelBase)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setWidths(modelVariants.stream()
                    .map(VehicleVariant::getWidth)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setLengths(modelVariants.stream()
                    .map(VehicleVariant::getLength)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSafetyFeatures(modelVariants.stream()
                    .map(VehicleVariant::getSafetyFeature)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setInfotainments(modelVariants.stream()
                    .map(VehicleVariant::getInfotainment)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setComforts(modelVariants.stream()
                    .map(VehicleVariant::getComfort)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSuffixes(modelVariants.stream()
                    .map(VehicleVariant::getSuffix)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setEngineCapacities(modelVariants.stream()
                    .map(VehicleVariant::getEngineCapacity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            modelDetails.put(mName, attributes);
        });

        return response;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty()) {
                    return null;
                }
                // Remove non-numeric suffixes (e.g., " km/h")
                str = str.replaceAll("[^0-9.]", "");
                if (str.isEmpty()) {
                    return null;
                }
                // Parse as Double first to handle "180.0" cases, then convert to Integer
                return Double.valueOf(str).intValue();
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            log.warn("Unsupported type for integer parsing: {}", value.getClass());
        } catch (NumberFormatException e) {
            log.error("Failed to parse integer from value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty()) {
                    return null;
                }
                // Remove non-numeric suffixes if needed
                str = str.replaceAll("[^0-9.]", "");
                if (str.isEmpty()) {
                    return null;
                }
                return Double.valueOf(str);
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            log.warn("Unsupported type for double parsing: {}", value.getClass());
        } catch (NumberFormatException e) {
            log.error("Failed to parse double from value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }
}