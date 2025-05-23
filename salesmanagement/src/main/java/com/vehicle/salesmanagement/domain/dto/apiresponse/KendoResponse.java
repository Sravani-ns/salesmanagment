package com.vehicle.salesmanagement.domain.dto.apiresponse;

import java.util.List;

public class KendoResponse<T> {
    private List<T> data;
    private long total;

    public KendoResponse(List<T> data, long total) {
        this.data = data;
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}