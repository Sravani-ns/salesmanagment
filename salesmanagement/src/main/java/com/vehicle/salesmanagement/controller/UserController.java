package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apiresponse.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "User Management", description = "APIs for managing user information")
public class UserController {
    @GetMapping
    @Operation(summary = "Get user info", description = "Retrieves the authenticated user's role and permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getUserInfo() {
        log.info("Received request for user info");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String role = authentication.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("ROLE_USER")
                    .replace("ROLE_", "");

            Map<String, Boolean> permissions = new HashMap<>();
            if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                permissions.put("viewTotalOrders", true);
            } else {
                permissions.put("viewTotalOrders", false);
            }
            // Permission for Pending Orders (accessible to all authenticated users)
            if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                permissions.put("viewPendingOrders", true);
            } else {
                permissions.put("viewPendingOrders", false);
            }
            // Permission for Finance Pending (accessible to all authenticated users)
            if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                permissions.put("viewFinancePending", true);
            } else {
                permissions.put("viewFinancePending", false);
            }
            // Permission for Closed Orders (accessible to all authenticated users)
            if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                permissions.put("viewClosedOrders", true);
            } else {
                permissions.put("viewClosedOrders", false);
            }


            UserInfoResponse userInfo = new UserInfoResponse();
            userInfo.setRole(role);
            userInfo.setPermissions(permissions);

            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "User info retrieved successfully",
                    userInfo
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving user info: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }
}