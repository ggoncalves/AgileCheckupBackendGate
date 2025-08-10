package com.agilecheckup.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API model for Category to be used in REST endpoints
 * This model represents the  structure for categories
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryApi {
    private String id;
    private String name;
    private String description;
    private String createdDate;
    private String lastUpdatedDate;
}