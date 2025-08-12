package com.agilecheckup.api.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API model for Pillar to be used in REST endpoints
 * This model represents the structure for pillars
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