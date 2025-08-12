package com.agilecheckup.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API model for Pillar to be used in REST endpoints
 * This model represents the  structure for pillars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PillarApi {
    private String id;
    private String name;
    private String description;
    private Map<String, CategoryApi> categoryMap;
    private String createdDate;
    private String lastUpdatedDate;
}