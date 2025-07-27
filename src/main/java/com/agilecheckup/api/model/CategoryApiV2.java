package com.agilecheckup.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API model for CategoryV2 to be used in REST endpoints
 * This model represents the V2 structure for categories
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryApiV2 {
    private String id;
    private String name;
    private String description;
    private String createdDate;
    private String lastUpdatedDate;
}