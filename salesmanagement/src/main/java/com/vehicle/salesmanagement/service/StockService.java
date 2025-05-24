package com.vehicle.salesmanagement.service;


import com.vehicle.salesmanagement.domain.dto.apirequest.StockDTO;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoResponse;
import com.vehicle.salesmanagement.domain.entity.model.StockDetails;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.StockDetailsRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockDetailsRepository stockDetailsRepository;

    public KendoResponse<StockDTO> getStockDetails() {
        List<StockDetails> stockDetails = stockDetailsRepository.findByStockStatus(StockStatus.AVAILABLE);
        List<StockDTO> stockDTOs = stockDetails.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new KendoResponse<>(stockDTOs, stockDTOs.size());
    }

//    private StockDTO mapToDTO(StockDetails stock) {
//        StockDTO dto = new StockDTO();
//        dto.setStockId(stock.getStockId());
//        dto.setModelName(stock.getVehicleModel().getModelName());
//        dto.setFuelType(stock.getFuelType());
//        dto.setTransmissionType(stock.getTransmissionType());
//        dto.setVariant(stock.getVariant());
//        dto.setQuantity(stock.getQuantity());
//        dto.setVinNumber(stock.getVinNumber());
//        dto.setSuffix(stock.getSuffix());
//        dto.setEngineColour(stock.getEngineColour());
//        dto.setColour(stock.getColour());
//        dto.setInteriorColour(stock.getInteriorColour());
//        return dto;
//    }

    @Transactional
    public StockDTO updateStock(@Valid StockDTO request) {
        StockDetails stock = stockDetailsRepository.findById(request.getStockId())
                .orElseThrow(() -> new RuntimeException("Stock not found with ID: " + request.getStockId()));

        // Update fields if provided
        if (request.getSuffix() != null) {
            stock.setSuffix(request.getSuffix());
        }
        if (request.getFuelType() != null) {
            stock.setFuelType(request.getFuelType());
        }
        if (request.getColour() != null) {
            stock.setColour(request.getColour());
        }
        if (request.getEngineColour() != null) {
            stock.setEngineColour(request.getEngineColour());
        }
        if (request.getTransmissionType() != null) {
            stock.setTransmissionType(request.getTransmissionType());
        }
        if (request.getVariant() != null) {
            stock.setVariant(request.getVariant());
        }
        if (request.getQuantity() != null) {
            stock.setQuantity(request.getQuantity());
        }
//        if (request.getStockStatus() != null) {
//            stock.setStockStatus(request.getStockStatus());
//        }
        if (request.getInteriorColour() != null) {
            stock.setInteriorColour(request.getInteriorColour());
        }
        if (request.getVinNumber() != null && !request.getVinNumber().equals(stock.getVinNumber())) {
            stockDetailsRepository.findByVinNumber(request.getVinNumber())
                    .ifPresent(existing -> {
                        throw new RuntimeException("VIN number " + request.getVinNumber() + " is already in use");
                    });
            stock.setVinNumber(request.getVinNumber());
        }
//        if (request.getUpdatedBy() != null) {
//            stock.setUpdatedBy(request.getUpdatedBy());
//            stock.setUpdatedAt(LocalDateTime.now());
//        }

        StockDetails updatedStock = stockDetailsRepository.save(stock);
        return mapToDTO(updatedStock);
    }

    private StockDTO mapToDTO(StockDetails stock) {
        StockDTO dto = new StockDTO();
        dto.setStockId(stock.getStockId());
        dto.setModelName(stock.getVehicleModel().getModelName());
        dto.setFuelType(stock.getFuelType());
        dto.setTransmissionType(stock.getTransmissionType());
        dto.setVariant(stock.getVariant());
        dto.setQuantity(stock.getQuantity());
        dto.setVinNumber(stock.getVinNumber());
        dto.setSuffix(stock.getSuffix());
        dto.setEngineColour(stock.getEngineColour());
        dto.setColour(stock.getColour());
        dto.setInteriorColour(stock.getInteriorColour());
        return dto;
    }
}