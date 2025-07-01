package com.agilecheckup.gate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for performance cycle summary endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceCycleSummaryResponse {
    
    private String companyId;
    private String companyName;
    private List<PerformanceCycleCard> performanceCycles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceCycleCard {
        private String id;
        private String name;
        private String description;
        private String status; // ACTIVE, UPCOMING, COMPLETED, EXPIRED
        private String startDate;
        private String endDate;
        private String createdDate;
        private String lastActivityDate;
        private Integer assessmentMatrixCount;
        private Integer totalEmployeeAssessments;
        private Integer completedAssessments;
        private Double completionPercentage;
    }
}