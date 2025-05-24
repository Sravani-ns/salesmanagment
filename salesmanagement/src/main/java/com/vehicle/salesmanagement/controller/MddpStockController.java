package com.vehicle.salesmanagement.controller;
import com.vehicle.salesmanagement.domain.dto.apirequest.MddpDto;
import com.vehicle.salesmanagement.domain.dto.apirequest.StockDTO;
import com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoResponse;
import com.vehicle.salesmanagement.service.MddpStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api")
public class MddpStockController {

    @Autowired
    private MddpStockService mddpStockService;

    @GetMapping("/mddpstock")
    @Operation(summary = "Get Mddp", description = "Retrieves all Mddp stock successfully")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mddp stock retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<KendoResponse<MddpDto>> getMddpStockDetails() {
        try {
            KendoResponse<MddpDto> response = mddpStockService.getMddpStockDetails();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    @PutMapping("/updatemddpstock")
    @Operation(summary = "Update mddpstock details", description = "Updates specific fields of a mddpstock entry based on stock ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "mddpstock updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "mddpStock not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<KendoResponse<MddpDto>> updateStockDetails(
            @Valid @RequestBody MddpDto request) {
        try {
            MddpDto updatedStock = mddpStockService.updateStock(request);
            KendoResponse<MddpDto> response = new KendoResponse<>(Collections.singletonList(updatedStock), 1);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse errorResponse = new ApiResponse(
                    HttpStatus.NOT_FOUND.value(),
                    e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new KendoResponse<>(Collections.emptyList(), 0));
        } catch (Exception e) {
            ApiResponse errorResponse = new ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new KendoResponse<>(Collections.emptyList(), 0));
        }
    }
}