package com.agilecheckup.api.handler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.agilecheckup.api.model.TeamResponse;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Department;
import com.agilecheckup.persistency.entity.Team;
import com.agilecheckup.service.DepartmentService;
import com.agilecheckup.service.TeamService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TeamRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/teams/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/teams/([^/]+)/?$");
  private final TeamService teamService;
  private final DepartmentService departmentService;
  private final ObjectMapper objectMapper;

  public TeamRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.teamService = serviceComponent.buildTeamService();
    this.departmentService = serviceComponent.buildDepartmentService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /teams
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll(input);
      }
      // GET /teams/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /teams
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /teams/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // DELETE /teams/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    }
    catch (Exception e) {
      context.getLogger().log("Error in team endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing team request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();

    if (queryParams != null && queryParams.containsKey("tenantId")) {
      String tenantId = queryParams.get("tenantId");
      String departmentId = queryParams.get("departmentId");

      // If departmentId is provided, filter by department
      if (departmentId != null) {
        List<Team> teams = teamService.findByDepartmentId(departmentId);
        List<TeamResponse> responses = enrichTeamsWithDepartments(teams);
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(responses));
      }
      // If only tenantId is provided, return all teams for that tenant
      else {
        List<Team> teams = teamService.findAllByTenantId(tenantId);
        List<TeamResponse> responses = enrichTeamsWithDepartments(teams);
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(responses));
      }
    }

    // No tenantId provided - return error for security
    return ResponseBuilder.buildResponse(400, "tenantId is required");
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Team> team = teamService.findById(id);

    if (team.isPresent()) {
      TeamResponse response = enrichTeamWithDepartment(team.get());
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));
    }
    else {
      return ResponseBuilder.buildResponse(404, "Team not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Team> team = teamService.create(
        (String) requestMap.get("tenantId"), (String) requestMap.get("name"), (String) requestMap.get("description"), (String) requestMap.get("departmentId")
    );

    if (team.isPresent()) {
      TeamResponse response = enrichTeamWithDepartment(team.get());
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(response));
    }
    else {
      return ResponseBuilder.buildResponse(400, "Failed to create team");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Team> team = teamService.update(
        id, (String) requestMap.get("tenantId"), (String) requestMap.get("name"), (String) requestMap.get("description"), (String) requestMap.get("departmentId")
    );

    if (team.isPresent()) {
      TeamResponse response = enrichTeamWithDepartment(team.get());
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));
    }
    else {
      return ResponseBuilder.buildResponse(404, "Team not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Team> team = teamService.findById(id);

    if (team.isPresent()) {
      teamService.deleteById(id);
      return ResponseBuilder.buildResponse(204, "");
    }
    else {
      return ResponseBuilder.buildResponse(404, "Team not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /teams/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }

  private TeamResponse enrichTeamWithDepartment(Team team) {
    Optional<Department> department = departmentService.findById(team.getDepartmentId());
    return TeamResponse.fromTeam(team, department.orElse(null));
  }

  private List<TeamResponse> enrichTeamsWithDepartments(List<Team> teams) {
    return teams.stream().map(this::enrichTeamWithDepartment).collect(Collectors.toList());
  }
}