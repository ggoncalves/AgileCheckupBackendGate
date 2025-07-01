package com.agilecheckup.gate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Team summary DTO for dashboard showing assessment completion statistics.
 * 
 * @author Claude (claude-sonnet-4-20250514)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSummary {
    
    private String teamId;
    private String teamName;
    private int totalEmployees;
    private int completedAssessments;
    private double completionPercentage;
    private Double averageScore;
}