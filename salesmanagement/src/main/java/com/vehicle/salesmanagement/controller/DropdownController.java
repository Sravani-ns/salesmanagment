package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DropdownVariantResponse;
import com.vehicle.salesmanagement.service.DropdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dropdown")
@Tag(name = "Dropdown Management")
public class DropdownController {

    private final DropdownService dropdownService;

    @GetMapping("/model-names")
    @Operation(summary = "Get model names", description = "Retrieves unique vehicle model names")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Model names retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getModelNames() {
        return handleRequest(() -> dropdownService.getModelNames(), "Model names");
    }

    @GetMapping("/variants")
    @Operation(summary = "Get variants", description = "Retrieves unique vehicle variants, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Variants retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getVariants(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getVariants(modelName), "Variants", modelName);
    }

    @GetMapping("/colours")
    @Operation(summary = "Get colours", description = "Retrieves unique vehicle colours, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Colours retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getColours(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getColours(modelName), "Colours", modelName);
    }

    @GetMapping("/engine-capacities")
    @Operation(summary = "Get engine capacities", description = "Retrieves unique engine capacities, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Engine capacities retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getEngineCapacities(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getEngineCapacities(modelName), "Engine capacities", modelName);
    }

    @GetMapping("/fuel-types")
    @Operation(summary = "Get fuel types", description = "Retrieves unique fuel types, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fuel types retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getFuelTypes(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getFuelTypes(modelName), "Fuel types", modelName);
    }

    @GetMapping("/transmission-types")
    @Operation(summary = "Get transmission types", description = "Retrieves unique transmission types, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transmission types retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getTransmissionTypes(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getTransmissionTypes(modelName), "Transmission types", modelName);
    }

    @GetMapping("/prices")
    @Operation(summary = "Get prices", description = "Retrieves unique vehicle prices, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prices retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getPrices(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getPrices(modelName), "Prices", modelName);
    }

    @GetMapping("/years-of-manufacture")
    @Operation(summary = "Get years of manufacture", description = "Retrieves unique years of manufacture, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Years of manufacture retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getYearsOfManufacture(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getYearsOfManufacture(modelName), "Years of manufacture", modelName);
    }

    @GetMapping("/body-types")
    @Operation(summary = "Get body types", description = "Retrieves unique body types, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Body types retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getBodyTypes(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getBodyTypes(modelName), "Body types", modelName);
    }

    @GetMapping("/fuel-tank-capacities")
    @Operation(summary = "Get fuel tank capacities", description = "Retrieves unique fuel tank capacities, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fuel tank capacities retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getFuelTankCapacities(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getFuelTankCapacities(modelName), "Fuel tank capacities", modelName);
    }

    @GetMapping("/number-of-airbags")
    @Operation(summary = "Get number of airbags", description = "Retrieves unique number of airbags, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Number of airbags retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getNumberOfAirbags(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getNumberOfAirbags(modelName), "Number of airbags", modelName);
    }

    @GetMapping("/mileage-cities")
    @Operation(summary = "Get mileage cities", description = "Retrieves unique city mileage values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mileage cities retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getMileageCities(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getMileageCities(modelName), "Mileage cities", modelName);
    }

    @GetMapping("/mileage-highways")
    @Operation(summary = "Get mileage highways", description = "Retrieves unique highway mileage values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mileage highways retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getMileageHighways(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getMileageHighways(modelName), "Mileage highways", modelName);
    }

    @GetMapping("/seating-capacities")
    @Operation(summary = "Get seating capacities", description = "Retrieves unique seating capacities, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seating capacities retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getSeatingCapacities(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getSeatingCapacities(modelName), "Seating capacities", modelName);
    }

    @GetMapping("/max-powers")
    @Operation(summary = "Get max powers", description = "Retrieves unique max power values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Max powers retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getMaxPowers(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getMaxPowers(modelName), "Max powers", modelName);
    }

    @GetMapping("/max-torques")
    @Operation(summary = "Get max torques", description = "Retrieves unique max torque values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Max torques retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getMaxTorques(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getMaxTorques(modelName), "Max torques", modelName);
    }

    @GetMapping("/top-speeds")
    @Operation(summary = "Get top speeds", description = "Retrieves unique top speed values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top speeds retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getTopSpeeds(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getTopSpeeds(modelName), "Top speeds", modelName);
    }

    @GetMapping("/wheel-bases")
    @Operation(summary = "Get wheel bases", description = "Retrieves unique wheel base values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wheel bases retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getWheelBases(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getWheelBases(modelName), "Wheel bases", modelName);
    }

    @GetMapping("/widths")
    @Operation(summary = "Get widths", description = "Retrieves unique width values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Widths retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getWidths(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getWidths(modelName), "Widths", modelName);
    }

    @GetMapping("/lengths")
    @Operation(summary = "Get lengths", description = "Retrieves unique length values, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lengths retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getLengths(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getLengths(modelName), "Lengths", modelName);
    }

    @GetMapping("/safety-features")
    @Operation(summary = "Get safety features", description = "Retrieves unique safety feature summaries, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Safety features retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getSafetyFeatures(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getSafetyFeatures(modelName), "Safety features", modelName);
    }

    @GetMapping("/infotainments")
    @Operation(summary = "Get infotainments", description = "Retrieves unique infotainment summaries, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Infotainments retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getInfotainments(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getInfotainments(modelName), "Infotainments", modelName);
    }

    @GetMapping("/comforts")
    @Operation(summary = "Get comforts", description = "Retrieves unique comfort summaries, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comforts retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getComforts(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getComforts(modelName), "Comforts", modelName);
    }

    @GetMapping("/suffixes")
    @Operation(summary = "Get suffixes", description = "Retrieves unique vehicle suffixes, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suffixes retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getSuffixes(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getSuffixes(modelName), "Suffixes", modelName);
    }

    @GetMapping("/engine-colours")
    @Operation(summary = "Get engine colours", description = "Retrieves unique engine colours, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Engine colours retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getEngineColours(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getEngineColours(modelName), "Engine colours", modelName);
    }

    @GetMapping("/interior-colours")
    @Operation(summary = "Get interior colours", description = "Retrieves unique interior colours, optionally filtered by model name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Interior colours retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Model name required when filtering",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getInteriorColours(@RequestParam(value = "modelName", required = false) String modelName) {
        return handleFilteredRequest(() -> dropdownService.getInteriorColours(modelName), "Interior colours", modelName);
    }

    private ResponseEntity<ApiResponse> handleRequest(java.util.function.Supplier<DropdownVariantResponse> supplier, String fieldName) {
        log.info("Received request for {}", fieldName.toLowerCase());
        try {
            DropdownVariantResponse response = supplier.get();
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.OK.value(),
                    fieldName + " retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving {}: {}", fieldName.toLowerCase(), e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private ResponseEntity<ApiResponse> handleFilteredRequest(java.util.function.Supplier<DropdownVariantResponse> supplier, String fieldName, String modelName) {
        log.info("Received request for {} with modelName: {}", fieldName.toLowerCase(), modelName);
        try {
            if (modelName != null && modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty when provided");
                ApiResponse apiResponse = new ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Model name cannot be empty",
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
            DropdownVariantResponse response = supplier.get();
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.OK.value(),
                    fieldName + (modelName != null ? " for model " + modelName : "") + " retrieved successfully",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving {}: {}", fieldName.toLowerCase(), e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}