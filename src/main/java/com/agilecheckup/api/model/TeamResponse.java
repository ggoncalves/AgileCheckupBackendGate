package com.agilecheckup.api.model;

import com.agilecheckup.persistency.entity.Department;
import com.agilecheckup.persistency.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private String id;
    private Date createdDate;
    private Date lastUpdatedDate;
    private String tenantId;
    private String name;
    private String description;
    private Department department;
    
    public static TeamResponse fromTeam(Team team, Department department) {
        return TeamResponse.builder()
                .id(team.getId())
                .createdDate(team.getCreatedDate())
                .lastUpdatedDate(team.getLastUpdatedDate())
                .tenantId(team.getTenantId())
                .name(team.getName())
                .description(team.getDescription())
                .department(department)
                .build();
    }
}