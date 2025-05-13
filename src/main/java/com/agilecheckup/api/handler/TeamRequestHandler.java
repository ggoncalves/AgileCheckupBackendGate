package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Team;
import com.agilecheckup.service.TeamService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class TeamRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/teams/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/teams/([^/]+)/?$");
  private final TeamService teamService;
  private final ObjectMapper objectMapper;

  public TeamRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.teamService = serviceComponent.buildTeamService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /teams
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
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
      // DELETE /teams/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in team endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing team request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(teamService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Team> team = teamService.findById(id);

    if (team.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(team.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Team not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Team> team = teamService.create(
        (String) requestMap.get("name"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("departmentId")
    );

    if (team.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(team.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create team");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Team> team = teamService.findById(id);

    if (team.isPresent()) {
      teamService.delete(team.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Team not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /teams/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}