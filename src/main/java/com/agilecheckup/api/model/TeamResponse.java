package com.agilecheckup.api.model;

import java.time.Instant;

import com.agilecheckup.persistency.entity.Department;
import com.agilecheckup.persistency.entity.Team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
  private String id;
  private Instant createdDate;
  private Instant lastUpdatedDate;
  private String tenantId;
  private String name;
  private String description;
  private String departmentId;
  private Department department;

  public static TeamResponse fromTeam(Team team, Department department) {
    return TeamResponse.builder().id(team.getId()).createdDate(team.getCreatedDate()).lastUpdatedDate(team.getLastUpdatedDate()).tenantId(team.getTenantId()).name(team.getName()).description(team.getDescription()).departmentId(team.getDepartmentId()).department(department).build();
  }

}