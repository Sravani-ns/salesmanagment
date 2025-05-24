package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.MddpDto;
import com.vehicle.salesmanagement.domain.dto.apirequest.StockDTO;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoResponse;
import com.vehicle.salesmanagement.domain.entity.model.MddpStock;
import com.vehicle.salesmanagement.domain.entity.model.StockDetails;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.MddpStockRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MddpStockService {

    @Autowired
    private MddpStockRepository mddpStockRepository;

    public KendoResponse<MddpDto> getMddpStockDetails() {
        List<MddpStock> mddpStocks = mddpStockRepository.findByStockStatus(StockStatus.AVAILABLE);
        List<MddpDto> mddpDTOs = mddpStocks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new KendoResponse<>(mddpDTOs, mddpDTOs.size());
    }


    @Transactional
    public MddpDto updateStock(@Valid MddpDto request) {
        MddpStock stock = mddpStockRepository.findById(request.getMddpOrderId())
                .orElseThrow(() -> new RuntimeException("MddpStock not found with ID: " + request.getMddpOrderId()));

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
            mddpStockRepository.findByVinNumber(request.getVinNumber())
                    .ifPresent(existing -> {
                        throw new RuntimeException("VIN number " + request.getVinNumber() + " is already in use");
                    });
            stock.setVinNumber(request.getVinNumber());
        }
//        if (request.getUpdatedBy() != null) {
//            stock.setUpdatedBy(request.getUpdatedBy());
//            stock.setUpdatedAt(LocalDateTime.now());
//        }

        MddpStock updatedStock = mddpStockRepository.save(stock);
        return mapToDTO(updatedStock);
    }

    private MddpDto mapToDTO(MddpStock stock) {
        MddpDto dto = new MddpDto();
        dto.setMddpOrderId(stock.getMddpOrderId());
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