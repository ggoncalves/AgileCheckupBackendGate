package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.AnalyticsScope;
import com.agilecheckup.persistency.entity.AssessmentMatrix;
import com.agilecheckup.persistency.entity.DashboardAnalytics;
import com.agilecheckup.service.AssessmentMatrixService;
import com.agilecheckup.service.DashboardAnalyticsService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private DashboardAnalyticsService dashboardAnalyticsService;

  @Mock
  private AssessmentMatrixService assessmentMatrixService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private DashboardAnalyticsRequestHandler handler;
  private ObjectMapper objectMapper;

  private static final String COMPANY_ID = "company123";
  private static final String ASSESSMENT_MATRIX_ID = "matrix456";
  private static final String TEAM_ID = "team789";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    when(serviceComponent.buildDashboardAnalyticsService()).thenReturn(dashboardAnalyticsService);
    when(serviceComponent.buildAssessmentMatrixService()).thenReturn(assessmentMatrixService);
    lenient().when(context.getLogger()).thenReturn(lambdaLogger);

    handler = new DashboardAnalyticsRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void handleRequest_OverviewEndpoint_WithValidTenantId_ShouldReturnOverview() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalytics();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(Arrays.asList(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    verify(dashboardAnalyticsService).getOverview(ASSESSMENT_MATRIX_ID);
  }

  @Test
  void handleRequest_OverviewEndpoint_WithoutTenantId_ShouldReturnBadRequest() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");
    request.setQueryStringParameters(new HashMap<>());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Missing required parameter: tenantId");
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_OverviewEndpoint_WithWrongTenantId_ShouldReturnForbidden() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "wrong-tenant");
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix(); // Has COMPANY_ID as tenantId
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBody()).contains("Access denied");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    // Should not call analytics service since access is denied
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_TeamEndpoint_WithValidData_ShouldReturnTeamAnalytics() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/team/" + ASSESSMENT_MATRIX_ID + "/" + TEAM_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalytics();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getTeamAnalytics(ASSESSMENT_MATRIX_ID, TEAM_ID)).thenReturn(Optional.of(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    verify(dashboardAnalyticsService).getTeamAnalytics(ASSESSMENT_MATRIX_ID, TEAM_ID);
  }

  @Test
  void handleRequest_PerformanceCycleSummaryEndpoint_WithValidTenantId_ShouldReturnSummary() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/performance-cycle-summary/" + COMPANY_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void handleRequest_InvalidPath_ShouldReturnNotFound() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/invalid/path");
    request.setHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Not Found");
  }

  @Test
  void handleRequest_PostMethod_ShouldReturnMethodNotAllowed() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("POST");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).contains("Method Not Allowed");
  }

  @Test
  void handleRequest_AnalyticsNotFound_ShouldReturnEmptyResponse() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains("\"generalAverage\":0.0");
    assertThat(response.getBody()).contains("\"totalEmployees\":0");
    assertThat(response.getBody()).contains("\"teams\":[]");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
  }

  @Test
  void handleRequest_TeamAnalyticsNotFound_ShouldReturnEmptyResponse() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/team/" + ASSESSMENT_MATRIX_ID + "/" + TEAM_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getTeamAnalytics(ASSESSMENT_MATRIX_ID, TEAM_ID)).thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains("\"teamId\":\"" + TEAM_ID + "\"");
    assertThat(response.getBody()).contains("\"totalScore\":0.0");
    assertThat(response.getBody()).contains("\"employeeCount\":0");
    assertThat(response.getBody()).contains("\"pillarScores\":{}");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
  }

  @Test
  void handleRequest_AssessmentMatrixNotFound_ShouldReturnNotFound() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.empty());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Assessment matrix not found");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_TeamEndpoint_WithWrongTenantId_ShouldReturnForbidden() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/team/" + ASSESSMENT_MATRIX_ID + "/" + TEAM_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "wrong-tenant");
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix(); // Has COMPANY_ID as tenantId
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBody()).contains("Access denied");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    // Should not call analytics service since access is denied at matrix level
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_ComputeEndpoint_WithValidTenantId_ShouldComputeAnalytics() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/compute/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("POST");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("\"message\":\"Dashboard analytics computed successfully\"");
    assertThat(response.getBody()).contains("\"assessmentMatrixId\":\"" + ASSESSMENT_MATRIX_ID + "\"");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    verify(dashboardAnalyticsService).updateAssessmentMatrixAnalytics(ASSESSMENT_MATRIX_ID);
  }

  @Test
  void handleRequest_ComputeEndpoint_WithWrongMethod_ShouldReturnMethodNotAllowed() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/compute/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).contains("Method Not Allowed - POST required for compute endpoint");
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_ComputeEndpoint_WithoutTenantId_ShouldReturnBadRequest() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/compute/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("POST");
    request.setQueryStringParameters(new HashMap<>());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Missing required parameter: tenantId");
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_ComputeEndpoint_WithWrongTenantId_ShouldReturnForbidden() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/compute/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("POST");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "wrong-tenant");
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix(); // Has COMPANY_ID as tenantId
    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBody()).contains("Access denied");
    verify(assessmentMatrixService).findById(ASSESSMENT_MATRIX_ID);
    verifyNoInteractions(dashboardAnalyticsService);
  }

  @Test
  void handleRequest_OverviewEndpoint_ShouldPopulateTopBottomPillarsAndCategories() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithPillarData();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(Arrays.asList(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Parse response to verify top/bottom pillars and categories are populated
    assertThat(response.getBody()).contains("\"topPillar\":");
    assertThat(response.getBody()).contains("\"bottomPillar\":");
    assertThat(response.getBody()).contains("\"topCategory\":");
    assertThat(response.getBody()).contains("\"bottomCategory\":");

    // Verify specific values based on our test data
    assertThat(response.getBody()).contains("\"name\":\"Team Collaboration\""); // Top pillar
    assertThat(response.getBody()).contains("\"percentage\":87.5"); // Top pillar percentage
    assertThat(response.getBody()).contains("\"name\":\"Technical Practices\""); // Bottom pillar
    assertThat(response.getBody()).contains("\"percentage\":65.0"); // Bottom pillar percentage
    assertThat(response.getBody()).contains("\"name\":\"Communication\""); // Top category
    assertThat(response.getBody()).contains("\"percentage\":90.0"); // Top category percentage
    assertThat(response.getBody()).contains("\"name\":\"Testing\""); // Bottom category
    assertThat(response.getBody()).contains("\"percentage\":60.0"); // Bottom category percentage
  }

  @Test
  void handleRequest_OverviewEndpoint_WithSinglePillar_ShouldSetSameTopBottom() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithSinglePillar();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(Arrays.asList(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Both top and bottom should be the same pillar/category
    assertThat(response.getBody()).contains("\"topPillar\":");
    assertThat(response.getBody()).contains("\"bottomPillar\":");
    assertThat(response.getBody()).contains("\"topCategory\":");
    assertThat(response.getBody()).contains("\"bottomCategory\":");

    // Should have same pillar for both top and bottom
    assertThat(response.getBody()).contains("\"name\":\"Only Pillar\"");
    assertThat(response.getBody()).contains("\"percentage\":75.0");
  }

  @Test
  void handleRequest_OverviewEndpoint_ShouldPopulatePillarScores() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithPillarScores();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(List.of(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Verify pillar scores are populated
    assertThat(response.getBody()).contains("\"pillarScores\":");
    assertThat(response.getBody()).contains("\"Team Collaboration\":");
    assertThat(response.getBody()).contains("\"Technical Practices\":");

    // Verify pillar structure
    assertThat(response.getBody()).contains("\"score\":87.5");
    assertThat(response.getBody()).contains("\"actualScore\":175.0");
    assertThat(response.getBody()).contains("\"potentialScore\":200.0");
    assertThat(response.getBody()).contains("\"gapFromPotential\":12.5");

    // Verify categories are populated
    assertThat(response.getBody()).contains("\"categories\":");
    assertThat(response.getBody()).contains("\"Communication\"");
    assertThat(response.getBody()).contains("\"Trust\"");
    assertThat(response.getBody()).contains("\"Code Quality\"");
    assertThat(response.getBody()).contains("\"Testing\"");
  }

  @Test
  void handleRequest_OverviewEndpoint_WithEmptyPillars_ShouldReturnEmptyPillarScores() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithEmptyPillars();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(List.of(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Verify empty pillar scores
    assertThat(response.getBody()).contains("\"pillarScores\":{}");
  }

  @Test
  void handleRequest_OverviewEndpoint_WithMalformedAnalyticsJson_ShouldHandleGracefully() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithMalformedJson();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(List.of(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Verify empty pillar scores for malformed JSON
    assertThat(response.getBody()).contains("\"pillarScores\":{}");
  }

  @Test
  void handleRequest_OverviewEndpoint_WithIncompleteCategories_ShouldHandlePartialData() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/dashboard-analytics/overview/" + ASSESSMENT_MATRIX_ID);
    request.setHttpMethod("GET");

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", COMPANY_ID);
    request.setQueryStringParameters(queryParams);

    AssessmentMatrix mockMatrix = createMockAssessmentMatrix();
    DashboardAnalytics mockAnalytics = createMockDashboardAnalyticsWithIncompleteCategories();

    when(assessmentMatrixService.findById(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockMatrix));
    when(dashboardAnalyticsService.getOverview(ASSESSMENT_MATRIX_ID)).thenReturn(Optional.of(mockAnalytics));
    when(dashboardAnalyticsService.getAllAnalytics(ASSESSMENT_MATRIX_ID)).thenReturn(Arrays.asList(mockAnalytics));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // Should handle incomplete data gracefully
    assertThat(response.getBody()).contains("\"pillarScores\":");
    assertThat(response.getBody()).contains("\"Team Collaboration\":");
  }

  private DashboardAnalytics createMockDashboardAnalytics() {
    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#TEAM#" + TEAM_ID).companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.TEAM).teamId(TEAM_ID).teamName("Test Team").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(85.5).employeeCount(10).completionPercentage(90.0).lastUpdated(Instant.now()).analyticsDataJson("{\"pillars\": {}, \"wordCloud\": {}}").build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithPillarData() {
    // Create comprehensive pillar data with multiple pillars and categories
    String analyticsJson = "{" + "\"pillars\": {" + "\"pillar1\": {" + "\"name\": \"Team Collaboration\"," + "\"percentage\": 87.5," + "\"actualScore\": 175.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 12.5," + "\"categories\": {" + "\"cat1\": {\"name\": \"Communication\", \"percentage\": 90.0, \"actualScore\": 45.0, \"potentialScore\": 50.0}," + "\"cat2\": {\"name\": \"Trust\", \"percentage\": 85.0, \"actualScore\": 85.0, \"potentialScore\": 100.0}" + "}" + "}," + "\"pillar2\": {" + "\"name\": \"Technical Practices\"," + "\"percentage\": 65.0," + "\"actualScore\": 130.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 35.0," + "\"categories\": {" + "\"cat3\": {\"name\": \"Code Quality\", \"percentage\": 70.0, \"actualScore\": 70.0, \"potentialScore\": 100.0}," + "\"cat4\": {\"name\": \"Testing\", \"percentage\": 60.0, \"actualScore\": 60.0, \"potentialScore\": 100.0}" + "}" + "}" + "}" + "}";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#ASSESSMENT_MATRIX").companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.ASSESSMENT_MATRIX).teamId(null).teamName("Overview").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(76.25) // Average of 87.5 and 65.0
        .employeeCount(10).completionPercentage(90.0).lastUpdated(Instant.now()).analyticsDataJson(analyticsJson).build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithSinglePillar() {
    // Create single pillar data for edge case testing
    String analyticsJson = "{" + "\"pillars\": {" + "\"pillar1\": {" + "\"name\": \"Only Pillar\"," + "\"percentage\": 75.0," + "\"actualScore\": 150.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 25.0," + "\"categories\": {" + "\"cat1\": {\"name\": \"Only Category\", \"percentage\": 75.0, \"actualScore\": 75.0, \"potentialScore\": 100.0}" + "}" + "}" + "}" + "}";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#ASSESSMENT_MATRIX").companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.ASSESSMENT_MATRIX).teamId(null).teamName("Overview").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(75.0).employeeCount(5).completionPercentage(100.0).lastUpdated(Instant.now()).analyticsDataJson(analyticsJson).build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithPillarScores() {
    // Create comprehensive pillar data with multiple pillars and categories
    String analyticsJson = "{" + "\"pillars\": {" + "\"pillar1\": {" + "\"name\": \"Team Collaboration\"," + "\"percentage\": 87.5," + "\"actualScore\": 175.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 12.5," + "\"categories\": {" + "\"cat1\": {\"name\": \"Communication\", \"percentage\": 90.0, \"actualScore\": 45.0, \"potentialScore\": 50.0}," + "\"cat2\": {\"name\": \"Trust\", \"percentage\": 85.0, \"actualScore\": 85.0, \"potentialScore\": 100.0}" + "}" + "}," + "\"pillar2\": {" + "\"name\": \"Technical Practices\"," + "\"percentage\": 65.0," + "\"actualScore\": 130.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 35.0," + "\"categories\": {" + "\"cat3\": {\"name\": \"Code Quality\", \"percentage\": 70.0, \"actualScore\": 70.0, \"potentialScore\": 100.0}," + "\"cat4\": {\"name\": \"Testing\", \"percentage\": 60.0, \"actualScore\": 60.0, \"potentialScore\": 100.0}" + "}" + "}" + "}" + "}";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#TEAM#" + TEAM_ID).companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.TEAM).teamId(TEAM_ID).teamName("Test Team").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(76.25).employeeCount(10).completionPercentage(90.0).lastUpdated(Instant.now()).analyticsDataJson(analyticsJson).build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithEmptyPillars() {
    String analyticsJson = "{\"pillars\": {}, \"wordCloud\": {}}";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#TEAM#" + TEAM_ID).companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.TEAM).teamId(TEAM_ID).teamName("Test Team").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(0.0).employeeCount(5).completionPercentage(0.0).lastUpdated(Instant.now()).analyticsDataJson(analyticsJson).build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithMalformedJson() {
    String malformedJson = "{\"pillars\": {incomplete json";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#TEAM#" + TEAM_ID).companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.TEAM).teamId(TEAM_ID).teamName("Test Team").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(50.0).employeeCount(3).completionPercentage(100.0).lastUpdated(Instant.now()).analyticsDataJson(malformedJson).build();
  }

  private DashboardAnalytics createMockDashboardAnalyticsWithIncompleteCategories() {
    // Missing some category fields to test robustness
    String analyticsJson = "{" + "\"pillars\": {" + "\"pillar1\": {" + "\"name\": \"Team Collaboration\"," + "\"percentage\": 80.0," + "\"actualScore\": 160.0," + "\"potentialScore\": 200.0," + "\"gapFromPotential\": 20.0," + "\"categories\": {" + "\"cat1\": {\"name\": \"Communication\", \"percentage\": 85.0}," + // Missing actualScore, potentialScore
        "\"cat2\": {\"percentage\": 75.0, \"actualScore\": 75.0, \"potentialScore\": 100.0}" + // Missing name
        "}" + "}" + "}" + "}";

    return DashboardAnalytics.builder().companyPerformanceCycleId(COMPANY_ID + "#cycle456").assessmentMatrixScopeId(ASSESSMENT_MATRIX_ID + "#TEAM#" + TEAM_ID).companyId(COMPANY_ID).performanceCycleId("cycle456").assessmentMatrixId(ASSESSMENT_MATRIX_ID).scope(AnalyticsScope.TEAM).teamId(TEAM_ID).teamName("Test Team").companyName("Test Company").performanceCycleName("Q4 2024 Assessment").assessmentMatrixName("Test Assessment Matrix").generalAverage(80.0).employeeCount(8).completionPercentage(75.0).lastUpdated(Instant.now()).analyticsDataJson(analyticsJson).build();
  }

  private AssessmentMatrix createMockAssessmentMatrix() {
    return AssessmentMatrix.builder().id(ASSESSMENT_MATRIX_ID).name("Test Assessment Matrix").description("Test Description").tenantId(COMPANY_ID).performanceCycleId("cycle456").build();
  }

}