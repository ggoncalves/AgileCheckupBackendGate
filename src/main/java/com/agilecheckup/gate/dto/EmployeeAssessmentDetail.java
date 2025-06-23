package com.agilecheckup.gate.dto;

import com.agilecheckup.persistency.entity.AssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Detailed employee assessment information for dashboard display.
 * 
 * @author Claude (claude-sonnet-4-20250514)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssessmentDetail {
    
    private String employeeAssessmentId;
    private String employeeName;
    private String employeeEmail;
    private String teamId;
    private String status;
    private Integer answeredQuestions;
    private Double currentScore;
    private LocalDateTime lastModified;
}