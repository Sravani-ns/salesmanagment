package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationError implements Serializable {
    private static final long serialVersionUID = 1L;

    private String field;
    private String error;
}