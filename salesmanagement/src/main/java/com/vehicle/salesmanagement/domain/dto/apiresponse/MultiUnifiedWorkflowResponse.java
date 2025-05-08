package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MultiUnifiedWorkflowResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int status;
    private String message;
    private List<UnifiedWorkflowResponse> unifiedResponses;

    public MultiUnifiedWorkflowResponse(int status, String message, List<UnifiedWorkflowResponse> unifiedResponses) {
        this.status = status;
        this.message = message;
        this.unifiedResponses = unifiedResponses;
    }
}