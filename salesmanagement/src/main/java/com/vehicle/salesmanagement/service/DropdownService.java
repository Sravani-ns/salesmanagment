package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apiresponse.DropdownVariantResponse;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DropdownService {

    private final VehicleVariantRepository vehicleVariantRepository;

    public DropdownVariantResponse getModelNames() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctModelNames());
    }

    public DropdownVariantResponse getVariants() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctVariants());
    }

    public DropdownVariantResponse getVariants(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctVariantsByModelName(modelName));
    }

    public DropdownVariantResponse getColours() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctColours());
    }

    public DropdownVariantResponse getColours(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctColoursByModelName(modelName));
    }

    public DropdownVariantResponse getEngineCapacities() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctEngineCapacities());
    }

    public DropdownVariantResponse getEngineCapacities(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctEngineCapacitiesByModelName(modelName));
    }

    public DropdownVariantResponse getFuelTypes() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctFuelTypes());
    }

    public DropdownVariantResponse getFuelTypes(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctFuelTypesByModelName(modelName));
    }

    public DropdownVariantResponse getTransmissionTypes() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctTransmissionTypes());
    }

    public DropdownVariantResponse getTransmissionTypes(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctTransmissionTypesByModelName(modelName));
    }

    public DropdownVariantResponse getPrices() {
        List<String> prices = vehicleVariantRepository.findDistinctPrices().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(prices);
    }

    public DropdownVariantResponse getPrices(String modelName) {
        List<String> prices = vehicleVariantRepository.findDistinctPricesByModelName(modelName).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(prices);
    }

    public DropdownVariantResponse getYearsOfManufacture() {
        List<String> years = vehicleVariantRepository.findDistinctYearsOfManufacture().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(years);
    }

    public DropdownVariantResponse getYearsOfManufacture(String modelName) {
        List<String> years = vehicleVariantRepository.findDistinctYearsOfManufactureByModelName(modelName).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(years);
    }

    public DropdownVariantResponse getBodyTypes() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctBodyTypes());
    }

    public DropdownVariantResponse getBodyTypes(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctBodyTypesByModelName(modelName));
    }

    public DropdownVariantResponse getFuelTankCapacities() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctFuelTankCapacities());
    }

    public DropdownVariantResponse getFuelTankCapacities(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctFuelTankCapacitiesByModelName(modelName));
    }

    public DropdownVariantResponse getNumberOfAirbags() {
        List<String> airbags = vehicleVariantRepository.findDistinctNumberOfAirbags().stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(airbags);
    }

    public DropdownVariantResponse getNumberOfAirbags(String modelName) {
        List<String> airbags = vehicleVariantRepository.findDistinctNumberOfAirbagsByModelName(modelName).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return new DropdownVariantResponse(airbags);
    }

    public DropdownVariantResponse getMileageCities() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMileageCities());
    }

    public DropdownVariantResponse getMileageCities(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMileageCitiesByModelName(modelName));
    }

    public DropdownVariantResponse getMileageHighways() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMileageHighways());
    }

    public DropdownVariantResponse getMileageHighways(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMileageHighwaysByModelName(modelName));
    }

    public DropdownVariantResponse getSeatingCapacities() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSeatingCapacities());
    }

    public DropdownVariantResponse getSeatingCapacities(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSeatingCapacitiesByModelName(modelName));
    }

    public DropdownVariantResponse getMaxPowers() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMaxPowers());
    }

    public DropdownVariantResponse getMaxPowers(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMaxPowersByModelName(modelName));
    }

    public DropdownVariantResponse getMaxTorques() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMaxTorques());
    }

    public DropdownVariantResponse getMaxTorques(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctMaxTorquesByModelName(modelName));
    }

    public DropdownVariantResponse getTopSpeeds() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctTopSpeeds());
    }

    public DropdownVariantResponse getTopSpeeds(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctTopSpeedsByModelName(modelName));
    }

    public DropdownVariantResponse getWheelBases() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctWheelBases());
    }

    public DropdownVariantResponse getWheelBases(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctWheelBasesByModelName(modelName));
    }

    public DropdownVariantResponse getWidths() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctWidths());
    }

    public DropdownVariantResponse getWidths(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctWidthsByModelName(modelName));
    }

    public DropdownVariantResponse getLengths() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctLengths());
    }

    public DropdownVariantResponse getLengths(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctLengthsByModelName(modelName));
    }

    public DropdownVariantResponse getSafetyFeatures() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSafetyFeatures());
    }

    public DropdownVariantResponse getSafetyFeatures(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSafetyFeaturesByModelName(modelName));
    }

    public DropdownVariantResponse getInfotainments() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctInfotainments());
    }

    public DropdownVariantResponse getInfotainments(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctInfotainmentsByModelName(modelName));
    }

    public DropdownVariantResponse getComforts() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctComforts());
    }

    public DropdownVariantResponse getComforts(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctComfortsByModelName(modelName));
    }

    public DropdownVariantResponse getSuffixes() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSuffixes());
    }

    public DropdownVariantResponse getSuffixes(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctSuffixesByModelName(modelName));
    }

    public DropdownVariantResponse getEngineColours() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctEngineColours());
    }

    public DropdownVariantResponse getEngineColours(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctEngineColoursByModelName(modelName));
    }

    public DropdownVariantResponse getInteriorColours() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctInteriorColours());
    }

    public DropdownVariantResponse getInteriorColours(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctInteriorColoursByModelName(modelName));
    }

    public DropdownVariantResponse getVinNumbers() {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctVinNumbers());
    }

    public DropdownVariantResponse getVinNumbers(String modelName) {
        return new DropdownVariantResponse(vehicleVariantRepository.findDistinctVinNumbersByModelName(modelName));
    }
}