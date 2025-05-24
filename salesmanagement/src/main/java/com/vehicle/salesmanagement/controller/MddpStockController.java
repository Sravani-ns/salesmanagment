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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mddp-stock")
public class MddpStockController {

    @Autowired
    private MddpStockService mddpStockService;

    @GetMapping
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
}