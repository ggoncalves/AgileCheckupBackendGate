package com.agilecheckup.gate.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for team-specific dashboard analytics endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsTeamResponse {

  private String teamId;
  private String teamName;
  private Double totalScore;
  private Integer employeeCount;
  private Double completionPercentage;
  private Map<String, PillarScore> pillarScores;
  private WordCloud wordCloud;  // Add word cloud at team level

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PillarScore {
    private String name;
    private Double score; // Percentage (actualScore/potentialScore * 100)
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
    private Double score; // Percentage (actualScore/potentialScore * 100)
    private Double actualScore;
    private Double potentialScore;
    private Double gapFromPotential;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WordCloud {
    private List<WordFrequency> words;
    private Integer totalResponses;
    private String status; // sufficient, limited, none
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WordFrequency {
    private String text;
    private Integer count;
  }
}