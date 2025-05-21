package com.vehicle.salesmanagement.service;


import com.vehicle.salesmanagement.domain.dto.apirequest.StockDTO;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoResponse;
import com.vehicle.salesmanagement.domain.entity.model.StockDetails;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.StockDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private StockDTO mapToDTO(StockDetails stock) {
        StockDTO dto = new StockDTO();
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