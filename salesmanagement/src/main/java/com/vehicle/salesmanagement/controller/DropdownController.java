package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleAttributesResponse;
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

    @GetMapping("/vehicle-attributes")
    @Operation(summary = "Get vehicle attributes", description = "Retrieves all vehicle attributes, optionally filtered by model name and variant")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle attributes retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getVehicleAttributes(
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "variant", required = false) String variant) {
        log.info("Received request for vehicle attributes with modelName: {}, variant: {}", modelName, variant);
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
            if (variant != null && variant.trim().isEmpty()) {
                log.error("Variant cannot be empty when provided");
                ApiResponse apiResponse = new ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Variant cannot be empty",
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }

            VehicleAttributesResponse response = dropdownService.getVehicleAttributes(modelName, variant);
            String message = buildResponseMessage(modelName, variant);
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.OK.value(),
                    message,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric data provided: {}", e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid numeric data: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving vehicle attributes: {}", e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private String buildResponseMessage(String modelName, String variant) {
        if (modelName != null && variant != null) {
            return "Vehicle attributes for model " + modelName + " and variant " + variant + " retrieved successfully";
        } else if (modelName != null) {
            return "Vehicle attributes for model " + modelName + " retrieved successfully";
        } else {
            return "All vehicle attributes retrieved successfully";
        }
    }
}