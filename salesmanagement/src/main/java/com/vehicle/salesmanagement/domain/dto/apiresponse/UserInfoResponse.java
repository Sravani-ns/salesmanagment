package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.Data;

import java.util.Map;

@Data
public class UserInfoResponse {
    private String role;
    private Map<String, Boolean> permissions;
}