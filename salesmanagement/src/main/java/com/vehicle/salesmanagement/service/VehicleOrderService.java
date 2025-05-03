package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleModelRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleVariantRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleModelResponse;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleOrderService {

    private final StockDetailsRepository stockRepository;
    private final MddpStockRepository mddpStockRepository;
    private final ManufacturerOrderRepository manufacturerOrderRepository;
    private final VehicleOrderDetailsRepository orderRepository;
    private final VehicleVariantRepository variantRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final HistoryService historyService;

    @Transactional
    public OrderResponse checkAndBlockStock(OrderRequest orderRequest) {
        VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + orderRequest.getVehicleVariantId()));
        List<StockDetails> stocks = stockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE);

        if (stocks.isEmpty()) {
            return placeManufacturerOrder(orderRequest);
        }

        StockDetails stock = stocks.stream()
                .filter(s -> s.getQuantity() >= orderRequest.getQuantity()
                        && s.getColour().equals(orderRequest.getColour())
                        && s.getFuelType().equals(orderRequest.getFuelType())
                        && s.getTransmissionType().equals(orderRequest.getTransmissionType()))
                .findFirst()
                .orElse(null);

        if (stock == null) {
            return placeManufacturerOrder(orderRequest);
        }

        stock.setQuantity(stock.getQuantity() - orderRequest.getQuantity());
        if (stock.getQuantity() == 0) {
            stock.setStockStatus(StockStatus.DEPLETED);
        }
        stockRepository.save(stock);

        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.BLOCKED);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    @Transactional
    public OrderResponse checkAndReserveMddpStock(OrderRequest orderRequest) {
        VehicleVariant vehicleVariant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + orderRequest.getVehicleVariantId()));
        Optional<MddpStock> mddpStockOptional = mddpStockRepository.findByVehicleVariantAndStockStatus(
                vehicleVariant, StockStatus.AVAILABLE);
        if (mddpStockOptional.isPresent()) {
            MddpStock mddpStock = mddpStockOptional.get();
            if (mddpStock.getQuantity() >= orderRequest.getQuantity()) {
                VehicleModel vehicleModel = vehicleModelRepository.findById(orderRequest.getVehicleModelId())
                        .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + orderRequest.getVehicleModelId()));

                StockDetails newStock = new StockDetails();
                newStock.setVehicleVariant(vehicleVariant);
                newStock.setVehicleModel(vehicleModel);
                newStock.setColour(orderRequest.getColour());
                newStock.setFuelType(orderRequest.getFuelType());
                newStock.setTransmissionType(orderRequest.getTransmissionType());
                newStock.setVariant(orderRequest.getVariant());
                newStock.setQuantity(orderRequest.getQuantity());
                newStock.setStockStatus(StockStatus.AVAILABLE);
                newStock.setCreatedAt(LocalDateTime.now());
                stockRepository.save(newStock);

                mddpStock.setQuantity(mddpStock.getQuantity() - orderRequest.getQuantity());
                if (mddpStock.getQuantity() == 0) {
                    mddpStock.setStockStatus(StockStatus.DEPLETED);
                }
                mddpStockRepository.save(mddpStock);

                OrderResponse response = mapToOrderResponse(orderRequest);
                response.setOrderStatus(OrderStatus.BLOCKED);
                response.setCreatedAt(LocalDateTime.now());
                return response;
            }
        }
        return placeManufacturerOrder(orderRequest);
    }

    @Transactional
    public OrderResponse placeManufacturerOrder(OrderRequest orderRequest) {
        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.PENDING);
        return response;
    }

    @Transactional
    public OrderResponse confirmOrder(OrderResponse orderResponse) {
        VehicleOrderDetails orderDetails = mapToOrderDetails(orderResponse);
        historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.CONFIRMED); // Pass new status
        orderDetails.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(orderDetails);
        return orderResponse;
    }

    @Transactional
    public OrderResponse notifyCustomerWithTentativeDelivery(OrderResponse orderResponse) {
        VehicleOrderDetails orderDetails = mapToOrderDetails(orderResponse);
        historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.NOTIFIED); // Pass new status
        orderDetails.setOrderStatus(OrderStatus.NOTIFIED);
        orderDetails.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orderDetails);
        orderResponse.setUpdatedAt(LocalDateTime.now());
        return orderResponse;
    }

    @Transactional
    public OrderResponse cancelOrder(Long customerOrderId) {
        VehicleOrderDetails orderDetails = orderRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + customerOrderId));

        if (orderDetails.getOrderStatus() == OrderStatus.COMPLETED || orderDetails.getOrderStatus() == OrderStatus.CANCELED) {
            throw new IllegalStateException("Order with ID " + customerOrderId + " cannot be canceled. Current status: " + orderDetails.getOrderStatus());
        }

        VehicleVariant variant = orderDetails.getVehicleVariant();
        VehicleModel model = orderDetails.getVehicleModel();
        List<StockDetails> stocks = stockRepository.findByVehicleVariantAndVehicleModel(variant, model);

        StockDetails stock = stocks.stream()
                .filter(s -> s.getColour().equals(orderDetails.getColour())
                        && s.getFuelType().equals(orderDetails.getFuelType())
                        && s.getTransmissionType().equals(orderDetails.getTransmissionType()))
                .findFirst()
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + orderDetails.getQuantity());
            stock.setStockStatus(StockStatus.AVAILABLE);
            stockRepository.save(stock);
        } else {
            StockDetails newStock = new StockDetails();
            newStock.setVehicleVariant(variant);
            newStock.setVehicleModel(model);
            newStock.setColour(orderDetails.getColour());
            newStock.setFuelType(orderDetails.getFuelType());
            newStock.setTransmissionType(orderDetails.getTransmissionType());
            newStock.setVariant(orderDetails.getVariant());
            newStock.setQuantity(orderDetails.getQuantity());
            newStock.setStockStatus(StockStatus.AVAILABLE);
            newStock.setCreatedAt(LocalDateTime.now());
            stockRepository.save(newStock);
        }

        historyService.saveOrderHistory(orderDetails, "system", OrderStatus.CANCELED); // Pass new status
        orderDetails.setOrderStatus(OrderStatus.CANCELED);
        orderDetails.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orderDetails);

        OrderResponse response = mapToOrderResponseFromDetails(orderDetails);
        response.setOrderStatus(OrderStatus.CANCELED);
        return response;
    }

    public OrderResponse mapToOrderResponse(OrderRequest request) {
        VehicleVariant variant = variantRepository.findById(request.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + request.getVehicleVariantId()));

        OrderResponse response = new OrderResponse();
        response.setVehicleModelId(request.getVehicleModelId());
        response.setVehicleVariantId(request.getVehicleVariantId());
        response.setCustomerName(request.getCustomerName());
        response.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "");
        response.setEmail(request.getEmail() != null ? request.getEmail() : "");
        response.setPermanentAddress(request.getPermanentAddress() != null ? request.getPermanentAddress() : "");
        response.setCurrentAddress(request.getCurrentAddress() != null ? request.getCurrentAddress() : "");
        response.setAadharNo(request.getAadharNo() != null ? request.getAadharNo() : "");
        response.setPanNo(request.getPanNo() != null ? request.getPanNo() : "");
        response.setModelName(request.getModelName());
        response.setFuelType(request.getFuelType());
        response.setColour(request.getColour());
        response.setTransmissionType(request.getTransmissionType());
        response.setVariant(request.getVariant());
        response.setQuantity(request.getQuantity());

        BigDecimal totalPrice = request.getTotalPrice();
        if (totalPrice == null) {
            BigDecimal quantity = new BigDecimal(request.getQuantity());
            totalPrice = variant.getPrice().multiply(quantity);
        }
        response.setTotalPrice(totalPrice);

        BigDecimal bookingAmount = request.getBookingAmount();
        if (bookingAmount == null) {
            bookingAmount = totalPrice.multiply(new BigDecimal("0.1"));
        }
        response.setBookingAmount(bookingAmount);

        response.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "");
        response.setCreatedAt(LocalDateTime.now());
        response.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system");
        response.setUpdatedBy(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
        return response;
    }

    private VehicleOrderDetails mapToOrderDetails(OrderResponse response) {
        VehicleModel vehicleModel = vehicleModelRepository.findById(response.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + response.getVehicleModelId()));
        VehicleVariant vehicleVariant = variantRepository.findById(response.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + response.getVehicleVariantId()));

        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
        orderDetails.setVehicleModel(vehicleModel);
        orderDetails.setVehicleVariant(vehicleVariant);
        orderDetails.setCustomerName(response.getCustomerName());
        orderDetails.setPhoneNumber(response.getPhoneNumber());
        orderDetails.setEmail(response.getEmail());
        orderDetails.setPermanentAddress(response.getPermanentAddress());
        orderDetails.setCurrentAddress(response.getCurrentAddress());
        orderDetails.setAadharNo(response.getAadharNo());
        orderDetails.setPanNo(response.getPanNo());
        orderDetails.setModelName(response.getModelName());
        orderDetails.setFuelType(response.getFuelType());
        orderDetails.setColour(response.getColour());
        orderDetails.setTransmissionType(response.getTransmissionType());
        orderDetails.setVariant(response.getVariant());
        orderDetails.setQuantity(response.getQuantity());
        orderDetails.setTotalPrice(response.getTotalPrice());
        orderDetails.setBookingAmount(response.getBookingAmount());
        orderDetails.setPaymentMode(response.getPaymentMode());
        orderDetails.setOrderStatus(response.getOrderStatus());
        orderDetails.setCreatedAt(response.getCreatedAt());
        orderDetails.setUpdatedAt(response.getUpdatedAt());
        orderDetails.setCreatedBy(response.getCreatedBy());
        orderDetails.setUpdatedBy(response.getUpdatedBy());
        return orderDetails;
    }

    private OrderResponse mapToOrderResponseFromDetails(VehicleOrderDetails orderDetails) {
        OrderResponse response = new OrderResponse();
        response.setVehicleModelId(orderDetails.getVehicleModel().getVehicleModelId());
        response.setVehicleVariantId(orderDetails.getVehicleVariant().getVehicleVariantId());
        response.setCustomerName(orderDetails.getCustomerName());
        response.setPhoneNumber(orderDetails.getPhoneNumber());
        response.setEmail(orderDetails.getEmail());
        response.setPermanentAddress(orderDetails.getPermanentAddress());
        response.setCurrentAddress(orderDetails.getCurrentAddress());
        response.setAadharNo(orderDetails.getAadharNo());
        response.setPanNo(orderDetails.getPanNo());
        response.setModelName(orderDetails.getModelName());
        response.setFuelType(orderDetails.getFuelType());
        response.setColour(orderDetails.getColour());
        response.setTransmissionType(orderDetails.getTransmissionType());
        response.setVariant(orderDetails.getVariant());
        response.setQuantity(orderDetails.getQuantity());
        response.setTotalPrice(orderDetails.getTotalPrice());
        response.setBookingAmount(orderDetails.getBookingAmount());
        response.setPaymentMode(orderDetails.getPaymentMode());
        response.setCreatedAt(orderDetails.getCreatedAt());
        response.setUpdatedAt(orderDetails.getUpdatedAt());
        response.setCreatedBy(orderDetails.getCreatedBy());
        response.setUpdatedBy(orderDetails.getUpdatedBy());
        response.setOrderStatus(OrderStatus.valueOf(orderDetails.getOrderStatus().name()));
        return response;
    }

    @Transactional
    public VehicleModelResponse addVehicleModel(VehicleModelRequest request) {
        if (vehicleModelRepository.findByModelName(request.getModelName()).isPresent()) {
            VehicleModelResponse response = new VehicleModelResponse(
                    request.getModelName(),
                    null,
                    "Vehicle model '" + request.getModelName() + "' already exists."
            );
            return response;
        }

        VehicleModel vehicleModel = new VehicleModel();
        vehicleModel.setModelName(request.getModelName());
        vehicleModel.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");
        vehicleModel.setUpdatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");
        vehicleModel.setCreatedAt(LocalDateTime.now());
        vehicleModel.setUpdatedAt(LocalDateTime.now());
        vehicleModel = vehicleModelRepository.save(vehicleModel);

        return new VehicleModelResponse(
                vehicleModel.getModelName(),
                vehicleModel.getVehicleModelId(),
                null
        );
    }

    @Transactional
    public void addVehicleStock(
            Long modelId,
            Long variantId,
            String suffix,
            String fuelType,
            String colour,
            String engineColour,
            String transmissionType,
            String variantName,
            int quantity,
            String interiorColour,
            String vinNumber,
            String createdBy
    ) {
        VehicleModel model = vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Vehicle model not found: " + modelId));
        VehicleVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Vehicle variant not found: " + variantId));

        StockDetails stock = new StockDetails();
        stock.setVehicleModel(model);
        stock.setVehicleVariant(variant);
        stock.setSuffix(suffix);
        stock.setFuelType(fuelType);
        stock.setColour(colour);
        stock.setEngineColour(engineColour);
        stock.setTransmissionType(transmissionType);
        stock.setVariant(variantName);
        stock.setQuantity(quantity);
        stock.setInteriorColour(interiorColour);
        stock.setVinNumber(vinNumber);
        stock.setStockStatus(StockStatus.AVAILABLE);
        stock.setCreatedAt(LocalDateTime.now());
        stock.setCreatedBy(createdBy);
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setUpdatedBy(createdBy);

        stockRepository.save(stock);
    }

    @Transactional
    public VehicleVariant addVariantToModel(VehicleVariantRequest request) {
        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle model not found with ID: " + request.getModelId()));

        VehicleVariant variant = new VehicleVariant();
        variant.setVehicleModel(model);
        variant.setVariant(request.getVariant());
        variant.setSuffix(request.getSuffix());
        variant.setSafetyFeature(request.getSafetyFeature());
        variant.setColour(request.getColour());
        variant.setEngineColour(request.getEngineColour());
        variant.setTransmissionType(request.getTransmissionType());
        variant.setInteriorColour(request.getInteriorColour());
        variant.setVinNumber(request.getVinNumber());
        variant.setEngineCapacity(request.getEngineCapacity());
        variant.setFuelType(request.getFuelType());
        variant.setPrice(request.getPrice());
        variant.setYearOfManufacture(request.getYearOfManufacture());
        variant.setBodyType(request.getBodyType());
        variant.setFuelTankCapacity(request.getFuelTankCapacity());
        variant.setSeatingCapacity(request.getSeatingCapacity());
        variant.setMaxPower(request.getMaxPower());
        variant.setMaxTorque(request.getMaxTorque());
        variant.setTopSpeed(request.getTopSpeed());
        variant.setWheelBase(request.getWheelBase());
        variant.setWidth(request.getWidth());
        variant.setLength(request.getLength());
        variant.setInfotainment(request.getInfotainment());
        variant.setComfort(request.getComfort());
        variant.setNumberOfAirBags(request.getNumberOfAirBags());
        variant.setMileageCity(request.getMileageCity());
        variant.setMileageHighway(request.getMileageHighway());
        variant.setCreatedBy(request.getCreatedBy());
        variant.setUpdatedBy(request.getCreatedBy());
        variant.setCreatedAt(LocalDateTime.now());
        variant.setUpdatedAt(LocalDateTime.now());

        return variantRepository.save(variant);
    }
}