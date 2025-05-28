package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Department;
import com.agilecheckup.persistency.entity.Team;
import com.agilecheckup.service.TeamService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private TeamService teamService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private TeamRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    lenient().doReturn(teamService).when(serviceComponent).buildTeamService();
    lenient().doReturn(lambdaLogger).when(context).getLogger();
    handler = new TeamRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void handleGetAll_whenTenantIdProvided_shouldReturnTeamsForTenant() {
    // Given
    String tenantId = "tenant-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    Department department = createDepartment("dept-1", "Engineering");
    Team team1 = createTeam("team-1", "Team Alpha", department, tenantId);
    Team team2 = createTeam("team-2", "Team Beta", department, tenantId);
    List<Team> teams = Arrays.asList(team1, team2);

    when(teamService.findAllByTenantId(tenantId)).thenReturn(teams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("team-1", "Team Alpha", "team-2", "Team Beta");
    verify(teamService).findAllByTenantId(tenantId);
    verify(teamService, never()).findAll();
  }

  @Test
  void handleGetAll_whenDepartmentIdAndTenantIdProvided_shouldReturnFilteredTeams() {
    // Given
    String tenantId = "tenant-123";
    String departmentId = "dept-1";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);
    queryParams.put("departmentId", departmentId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    Department department = createDepartment(departmentId, "Engineering");
    Team team1 = createTeam("team-1", "Team Alpha", department, tenantId);
    List<Team> filteredTeams = Collections.singletonList(team1);

    when(teamService.findByDepartmentId(departmentId, tenantId)).thenReturn(filteredTeams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("team-1", "Team Alpha");
    verify(teamService).findByDepartmentId(departmentId, tenantId);
    verify(teamService, never()).findAllByTenantId(tenantId);
    verify(teamService, never()).findAll();
  }

  @Test
  void handleGetAll_whenNoTenantIdProvided_shouldReturnBadRequest() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("tenantId is required");
    verify(teamService, never()).findAll();
    verify(teamService, never()).findAllByTenantId(anyString());
    verify(teamService, never()).findByDepartmentId(anyString(), anyString());
  }

  @Test
  void handleGetById_whenTeamExists_shouldReturnTeam() {
    // Given
    String teamId = "team-123";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("GET");

    Department department = createDepartment("dept-1", "Engineering");
    Team team = createTeam(teamId, "Team Alpha", department, "tenant-123");

    when(teamService.findById(teamId)).thenReturn(Optional.of(team));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains(teamId, "Team Alpha");
    verify(teamService).findById(teamId);
  }

  @Test
  void handleGetById_whenTeamNotFound_shouldReturn404() {
    // Given
    String teamId = "non-existent";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("GET");

    when(teamService.findById(teamId)).thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).isEqualTo("Team not found");
    verify(teamService).findById(teamId);
  }

  @Test
  void handleCreate_withValidData_shouldCreateTeam() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"New Team\",\n" +
        "  \"description\": \"A new development team\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"departmentId\": \"dept-1\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("POST")
        .withBody(requestBody);

    Department department = createDepartment("dept-1", "Engineering");
    Team createdTeam = createTeam("new-team-id", "New Team", department, "tenant-123");
    createdTeam.setDescription("A new development team");

    when(teamService.create("New Team", "A new development team", "tenant-123", "dept-1"))
        .thenReturn(Optional.of(createdTeam));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-team-id", "New Team");
    verify(teamService).create("New Team", "A new development team", "tenant-123", "dept-1");
  }

  @Test
  void handleCreate_whenCreationFails_shouldReturn400() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"New Team\",\n" +
        "  \"description\": \"A new development team\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"departmentId\": \"dept-1\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("POST")
        .withBody(requestBody);

    when(teamService.create(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("Failed to create team");
  }

  @Test
  void handleUpdate_whenTeamExists_shouldUpdateTeam() {
    // Given
    String teamId = "team-123";
    String requestBody = "{\n" +
        "  \"name\": \"Updated Team\",\n" +
        "  \"description\": \"Updated description\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"departmentId\": \"dept-2\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("PUT")
        .withBody(requestBody);

    Department department = createDepartment("dept-2", "Operations");
    Team updatedTeam = createTeam(teamId, "Updated Team", department, "tenant-123");
    updatedTeam.setDescription("Updated description");

    when(teamService.update(teamId, "Updated Team", "Updated description", "tenant-123", "dept-2"))
        .thenReturn(Optional.of(updatedTeam));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains(teamId, "Updated Team");
    verify(teamService).update(teamId, "Updated Team", "Updated description", "tenant-123", "dept-2");
  }

  @Test
  void handleUpdate_whenTeamNotFound_shouldReturn404() {
    // Given
    String teamId = "non-existent";
    String requestBody = "{\n" +
        "  \"name\": \"Updated Team\",\n" +
        "  \"description\": \"Updated description\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"departmentId\": \"dept-2\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("PUT")
        .withBody(requestBody);

    when(teamService.update(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).isEqualTo("Team not found or update failed");
  }

  @Test
  void handleDelete_whenTeamExists_shouldDeleteTeam() {
    // Given
    String teamId = "team-123";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("DELETE");

    Department department = createDepartment("dept-1", "Engineering");
    Team team = createTeam(teamId, "Team to Delete", department, "tenant-123");

    when(teamService.findById(teamId)).thenReturn(Optional.of(team));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(response.getBody()).isEmpty();
    verify(teamService).findById(teamId);
    verify(teamService).delete(team);
  }

  @Test
  void handleDelete_whenTeamNotFound_shouldReturn404() {
    // Given
    String teamId = "non-existent";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams/" + teamId)
        .withHttpMethod("DELETE");

    when(teamService.findById(teamId)).thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).isEqualTo("Team not found");
    verify(teamService).findById(teamId);
    verify(teamService, never()).delete(any(Team.class));
  }

  @Test
  void handleRequest_withInvalidMethod_shouldReturn405() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("PATCH");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).isEqualTo("Method Not Allowed");
  }

  @Test
  void handleRequest_whenExceptionOccurs_shouldReturn500() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("GET")
        .withQueryStringParameters(Collections.singletonMap("tenantId", "tenant-123"));

    when(teamService.findAllByTenantId(anyString()))
        .thenThrow(new RuntimeException("Database connection failed"));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error processing team request");
    verify(lambdaLogger).log(contains("Database connection failed"));
  }

  @Test
  void handleCreate_withMissingFields_shouldReturn400() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Incomplete Team\"\n" +
        "}"; // Missing required fields: description, tenantId, departmentId

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // Mock service to return empty when called with null values
    when(teamService.create(anyString(), isNull(), isNull(), isNull()))
        .thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400); // Missing required fields should return bad request
    assertThat(response.getBody()).isEqualTo("Failed to create team");
    verify(teamService).create("Incomplete Team", null, null, null);
  }

  @Test
  void handleGetAll_withEmptyQueryParams_shouldRequireTenantId() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/teams")
        .withHttpMethod("GET")
        .withQueryStringParameters(new HashMap<>());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("tenantId is required");
  }

  // Helper methods
  private Department createDepartment(String id, String name) {
    Department department = new Department();
    department.setId(id);
    department.setName(name);
    department.setDescription(name + " Department");
    return department;
  }

  private Team createTeam(String id, String name, Department department, String tenantId) {
    Team team = new Team();
    team.setId(id);
    team.setName(name);
    team.setDepartment(department);
    team.setTenantId(tenantId);
    team.setDescription(name + " Description");
    return team;
  }
}