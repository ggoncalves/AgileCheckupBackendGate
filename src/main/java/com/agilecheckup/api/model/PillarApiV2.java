package com.agilecheckup.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API model for PillarV2 to be used in REST endpoints
 * This model represents the V2 structure for pillars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PillarApiV2 {
    private String id;
    private String name;
    private String description;
    private Map<String, CategoryApiV2> categoryMap;
    private String createdDate;
    private String lastUpdatedDate;
}