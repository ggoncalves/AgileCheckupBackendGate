package com.agilecheckup.gate.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for dashboard analytics overview endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsOverviewResponse {

  private Metadata metadata;
  private Summary summary;
  private List<TeamOverview> teams;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Metadata {
    private String assessmentMatrixId;
    private String companyName;
    private String performanceCycle;
    private String assessmentMatrixName;
    private String lastUpdated;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Summary {
    private Double generalAverage;
    private PillarSummary topPillar;
    private PillarSummary bottomPillar;
    private CategorySummary topCategory;
    private CategorySummary bottomCategory;
    private Integer totalEmployees;
    private Double completionPercentage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeamOverview {
    private String teamId;
    private String teamName;
    private Double totalScore;  // Renamed from overallPercentage
    private Integer employeeCount;
    private Double completionPercentage;
    private Map<String, PillarScore> pillarScores;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PillarScore {
    private String name;
    private Double score;           // Percentage (actualScore/potentialScore * 100)
    private Double actualScore;
    private Double potentialScore;
    private Double gapFromPotential;
    private List<CategoryScore> categories;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CategoryScore {
    private String name;
    private Double score;           // Percentage (actualScore/potentialScore * 100)
    private Double actualScore;
    private Double potentialScore;
    private Double gapFromPotential;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PillarSummary {
    private String name;
    private Double percentage;
    private Double actualScore;
    private Double potentialScore;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CategorySummary {
    private String name;
    private String pillar;
    private Double percentage;
    private Double actualScore;
    private Double potentialScore;
  }
}