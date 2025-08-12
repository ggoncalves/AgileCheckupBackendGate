package com.agilecheckup.gate.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for assessment matrix dashboard endpoint.
 * Contains both team summary and paginated employee details.
 * 
 * @author Claude (claude-sonnet-4-20250514)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

  private String matrixId;
  private String matrixName;
  private Object potentialScore;
  private List<TeamSummary> teamSummaries;
  private EmployeePageResponse employees;
  private int totalEmployees;
  private int completedAssessments;
}