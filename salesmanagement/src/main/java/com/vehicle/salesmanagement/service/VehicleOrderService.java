package com.vehicle.salesmanagement.service;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderDetailsResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.TotalOrdersResponse;
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
import java.util.ArrayList;
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
        historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.CONFIRMED);
        orderDetails.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(orderDetails);
        return orderResponse;
    }

    public OrderResponse notifyCustomerWithTentativeDelivery(OrderResponse orderResponse) {
        VehicleOrderDetails orderDetails = mapToOrderDetails(orderResponse);
        historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.NOTIFIED);
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

        historyService.saveOrderHistory(orderDetails, "system", OrderStatus.CANCELED);
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
            return new VehicleModelResponse(
                    request.getModelName(),
                    null,
                    "Vehicle model '" + request.getModelName() + "' already exists."
            );
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
    public List<VehicleModelResponse> addBulkVehicleModel(List<VehicleModelRequest> requests) {
        List<VehicleModelResponse> responses = new ArrayList<>();
        for (VehicleModelRequest request : requests) {
            try {
                VehicleModelResponse response = addVehicleModel(request);
                responses.add(response);
            } catch (Exception e) {
                responses.add(new VehicleModelResponse(
                        request.getModelName(),
                        null,
                        "Failed to add vehicle model: " + e.getMessage()
                ));
            }
        }
        return responses;
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
    public List<String> addBulkVehicleStock(List<StockAddRequest> requests) {
        List<String> messages = new ArrayList<>();
        for (StockAddRequest request : requests) {
            try {
                addVehicleStock(
                        request.getModelId(),
                        request.getVariantId(),
                        request.getSuffix(),
                        request.getFuelType(),
                        request.getColour(),
                        request.getEngineColour(),
                        request.getTransmissionType(),
                        request.getVariantName(),
                        request.getQuantity(),
                        request.getInteriorColour(),
                        request.getVinNumber(),
                        request.getCreatedBy()
                );
                messages.add("Stock added successfully for VIN: " + request.getVinNumber());
            } catch (Exception e) {
                messages.add("Failed to add stock for VIN " + request.getVinNumber() + ": " + e.getMessage());
            }
        }
        return messages;
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

    @Transactional
    public List<VehicleVariant> addBulkVariantToModel(List<VehicleVariantRequest> requests) {
        List<VehicleVariant> savedVariants = new ArrayList<>();
        for (VehicleVariantRequest request : requests) {
            try {
                VehicleVariant variant = addVariantToModel(request);
                savedVariants.add(variant);
            } catch (Exception e) {
                // Log the error or handle it as needed
                System.err.println("Failed to add variant for model ID " + request.getModelId() + ": " + e.getMessage());
            }
        }
        return savedVariants;
    }

    @Transactional
    public void addMddpStock(MddpStockAddRequest request) {
        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle model not found: " + request.getModelId()));
        VehicleVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle variant not found: " + request.getVariantId()));

        MddpStock stock = new MddpStock();
        stock.setVehicleModel(model);
        stock.setVehicleVariant(variant);
        stock.setSuffix(request.getSuffix());
        stock.setFuelType(request.getFuelType());
        stock.setColour(request.getColour());
        stock.setEngineColour(request.getEngineColour());
        stock.setTransmissionType(request.getTransmissionType());
        stock.setVariant(request.getGrade());
        stock.setQuantity(request.getQuantity());
        stock.setStockStatus(request.getStockStatus());
        stock.setExpectedDispatchDate(request.getExpectedDispatchDate());
        stock.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        stock.setInteriorColour(request.getInteriorColour());
        stock.setVinNumber(request.getVin());
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setCreatedBy(request.getCreatedBy());
        stock.setUpdatedBy(request.getCreatedBy());

        mddpStockRepository.save(stock);
    }

    @Transactional
    public List<String> addBulkMddpStock(List<MddpStockAddRequest> requests) {
        List<String> messages = new ArrayList<>();
        for (MddpStockAddRequest request : requests) {
            try {
                addMddpStock(request);
                messages.add("MDDP stock added successfully for VIN: " + request.getVin());
            } catch (Exception e) {
                messages.add("Failed to add MDDP stock for VIN " + request.getVin() + ": " + e.getMessage());
            }
        }
        return messages;
    }






//    @Transactional
//    public VehicleModelResponse addVehicleModel(VehicleModelRequest request) {
//        if (vehicleModelRepository.findByModelName(request.getModelName()).isPresent()) {
//            VehicleModelResponse response = new VehicleModelResponse(
//                    request.getModelName(),
//                    null,
//                    "Vehicle model '" + request.getModelName() + "' already exists."
//            );
//            return response;
//        }
//
//        VehicleModel vehicleModel = new VehicleModel();
//        vehicleModel.setModelName(request.getModelName());
//        vehicleModel.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");
//        vehicleModel.setUpdatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");
//        vehicleModel.setCreatedAt(LocalDateTime.now());
//        vehicleModel.setUpdatedAt(LocalDateTime.now());
//        vehicleModel = vehicleModelRepository.save(vehicleModel);
//
//        return new VehicleModelResponse(
//                vehicleModel.getModelName(),
//                vehicleModel.getVehicleModelId(),
//                null
//        );
//    }
//
//    @Transactional
//    public void addVehicleStock(
//            Long modelId,
//            Long variantId,
//            String suffix,
//            String fuelType,
//            String colour,
//            String engineColour,
//            String transmissionType,
//            String variantName,
//            int quantity,
//            String interiorColour,
//            String vinNumber,
//            String createdBy
//    ) {
//        VehicleModel model = vehicleModelRepository.findById(modelId)
//                .orElseThrow(() -> new RuntimeException("Vehicle model not found: " + modelId));
//        VehicleVariant variant = variantRepository.findById(variantId)
//                .orElseThrow(() -> new RuntimeException("Vehicle variant not found: " + variantId));
//
//        StockDetails stock = new StockDetails();
//        stock.setVehicleModel(model);
//        stock.setVehicleVariant(variant);
//        stock.setSuffix(suffix);
//        stock.setFuelType(fuelType);
//        stock.setColour(colour);
//        stock.setEngineColour(engineColour);
//        stock.setTransmissionType(transmissionType);
//        stock.setVariant(variantName);
//        stock.setQuantity(quantity);
//        stock.setInteriorColour(interiorColour);
//        stock.setVinNumber(vinNumber);
//        stock.setStockStatus(StockStatus.AVAILABLE);
//        stock.setCreatedAt(LocalDateTime.now());
//        stock.setCreatedBy(createdBy);
//        stock.setUpdatedAt(LocalDateTime.now());
//        stock.setUpdatedBy(createdBy);
//
//        stockRepository.save(stock);
//    }
//
//    @Transactional
//    public VehicleVariant addVariantToModel(VehicleVariantRequest request) {
//        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
//                .orElseThrow(() -> new RuntimeException("Vehicle model not found with ID: " + request.getModelId()));
//
//        VehicleVariant variant = new VehicleVariant();
//        variant.setVehicleModel(model);
//        variant.setVariant(request.getVariant());
//        variant.setSuffix(request.getSuffix());
//        variant.setSafetyFeature(request.getSafetyFeature());
//        variant.setColour(request.getColour());
//        variant.setEngineColour(request.getEngineColour());
//        variant.setTransmissionType(request.getTransmissionType());
//        variant.setInteriorColour(request.getInteriorColour());
//        variant.setVinNumber(request.getVinNumber());
//        variant.setEngineCapacity(request.getEngineCapacity());
//        variant.setFuelType(request.getFuelType());
//        variant.setPrice(request.getPrice());
//        variant.setYearOfManufacture(request.getYearOfManufacture());
//        variant.setBodyType(request.getBodyType());
//        variant.setFuelTankCapacity(request.getFuelTankCapacity());
//        variant.setSeatingCapacity(request.getSeatingCapacity());
//        variant.setMaxPower(request.getMaxPower());
//        variant.setMaxTorque(request.getMaxTorque());
//        variant.setTopSpeed(request.getTopSpeed());
//        variant.setWheelBase(request.getWheelBase());
//        variant.setWidth(request.getWidth());
//        variant.setLength(request.getLength());
//        variant.setInfotainment(request.getInfotainment());
//        variant.setComfort(request.getComfort());
//        variant.setNumberOfAirBags(request.getNumberOfAirBags());
//        variant.setMileageCity(request.getMileageCity());
//        variant.setMileageHighway(request.getMileageHighway());
//        variant.setCreatedBy(request.getCreatedBy());
//        variant.setUpdatedBy(request.getCreatedBy());
//        variant.setCreatedAt(LocalDateTime.now());
//        variant.setUpdatedAt(LocalDateTime.now());
//
//        return variantRepository.save(variant);
//    }

    public int getBookedVehicleCount(Long customerOrderId) {
        VehicleOrderDetails orderDetails = orderRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + customerOrderId));
        return orderDetails.getQuantity();
    }


    public TotalOrdersResponse getTotalOrders() {
        Long totalOrders = orderRepository.countTotalOrders();
        return new TotalOrdersResponse(totalOrders);
    }


    public TotalOrdersResponse getPendingOrdersCount() {
        Long pendingOrders = orderRepository.countPendingOrders();
        return new TotalOrdersResponse(pendingOrders);
    }


    public TotalOrdersResponse getFinancePendingOrdersCount() {
        Long financePendingOrders = orderRepository.countFinancePendingOrders();
        return new TotalOrdersResponse(financePendingOrders);
    }

    public TotalOrdersResponse getClosedOrdersCount() {
        Long closedOrders = orderRepository.countClosedOrders();
        return new TotalOrdersResponse(closedOrders);
    }


    public OrderDetailsResponse getOrderDetailsByCustomerOrderId(Long customerOrderId) {
        VehicleOrderDetails orderDetails = orderRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + customerOrderId));

        OrderDetailsResponse response = new OrderDetailsResponse();
        response.setCustomerName(orderDetails.getCustomerName());
        response.setPhoneNumber(orderDetails.getPhoneNumber());
        response.setEmail(orderDetails.getEmail());
        response.setAadharNo(orderDetails.getAadharNo());
        response.setPanNo(orderDetails.getPanNo());
        response.setModelName(orderDetails.getModelName());
        response.setFuelType(orderDetails.getFuelType());
        response.setColour(orderDetails.getColour());
        response.setVariant(orderDetails.getVariant());
        response.setQuantity(orderDetails.getQuantity());
        response.setOrderStatus(orderDetails.getOrderStatus());

        return response;
    }
    }
