package com.agilecheckup.gate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for employee assessment details.
 * 
 * @author Claude (claude-sonnet-4-20250514)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePageResponse {
    
    private List<EmployeeAssessmentDetail> content;
    private int page;
    private int pageSize;
    private int totalCount;
}