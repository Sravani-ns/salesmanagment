package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.MddpDto;
import com.vehicle.salesmanagement.domain.dto.apirequest.StockDTO;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoResponse;
import com.vehicle.salesmanagement.domain.entity.model.MddpStock;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.MddpStockRepository;
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

    private MddpDto mapToDTO(MddpStock stock) {
        MddpDto dto = new MddpDto();
        dto.setMddpOrderId(stock.getMddpOrderId());
        dto.setModelName(stock.getVehicleModel().getModelName());
        dto.setFuelType(stock.getFuelType());
        dto.setTransmissionType(stock.getTransmissionType());
        dto.setVariant(stock.getGrade()); // Mapping 'grade' to 'variant'
        dto.setQuantity(stock.getQuantity());
        dto.setVinNumber(stock.getVin()); // Mapping 'vin' to 'vinNumber'
        dto.setSuffix(stock.getSuffix());
        dto.setEngineColour(stock.getEngineColour());
        dto.setColour(stock.getColour());
        dto.setInteriorColour(stock.getInteriorColour());
        return dto;
    }
}