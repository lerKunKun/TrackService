package com.logistics.track17.dto;

import lombok.Data;

@Data
public class ProductProcurementStatsDTO {
    private Long totalCount;
    private Long completeCount;
    private Long incompleteCount;
}
