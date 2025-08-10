package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.gate.cache.CacheManager;
import com.agilecheckup.persistency.entity.AssessmentConfiguration;
import com.agilecheckup.persistency.entity.AssessmentMatrix;
import com.agilecheckup.persistency.entity.Category;
import com.agilecheckup.persistency.entity.Pillar;
import com.agilecheckup.persistency.entity.QuestionNavigationType;
import com.agilecheckup.persistency.entity.score.PotentialScore;
import com.agilecheckup.service.AssessmentMatrixService;
import com.agilecheckup.service.dto.EmployeeAssessmentSummary;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssessmentMatrixRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private AssessmentMatrixService assessmentMatrixService;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Context context;

  @Captor
  ArgumentCaptor<AssessmentConfiguration> configurationCaptor;

  @Captor
  ArgumentCaptor<Map<String, Pillar>> pillarMapCaptor;
  @Mock
  private com.amazonaws.services.lambda.runtime.LambdaLogger lambdaLogger;


  private AssessmentMatrixRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    lenient().doReturn(assessmentMatrixService).when(serviceComponent).buildAssessmentMatrixService();
    lenient().doReturn(lambdaLogger).when(context).getLogger();

    // Mock cache manager to always return empty (no cache hits)
    lenient().doReturn(Optional.empty()).when(cacheManager).get(anyString(), any(Class.class));

    handler = new AssessmentMatrixRequestHandler(serviceComponent, objectMapper, cacheManager);
  }

  @Test
  void shouldSuccessfullyCreateAssessmentMatrixWithManualMapBuilding() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {\n" +
        "    \"p1\": {\n" +
        "      \"name\": \"Technical Skills\",\n" +
        "      \"description\": \"Technical knowledge and abilities\",\n" +
        "      \"categoryMap\": {\n" +
        "        \"c1\": {\n" +
        "          \"name\": \"Programming\",\n" +
        "          \"description\": \"Programming skills and knowledge\"\n" +
        "        },\n" +
        "        \"c2\": {\n" +
        "          \"name\": \"Architecture\",\n" +
        "          \"description\": \"System design and architecture\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // Create a sample assessment matrix to return
    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    // Set up the service to return the assessment matrix, capture arguments
    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When - This will be using our new implementation with manual map building
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        eq("Engineering Matrix"),
        eq("Competency assessment for engineering roles"),
        eq("tenant-test-id-123"),
        eq("3837551b-20f2-41eb-9779-8203a5209c45"),
        pillarMapCaptor.capture(),
        eq(null));

    // Verify the pillar map was built correctly
    Map<String, Pillar> capturedPillarMap = pillarMapCaptor.getValue();
    assertThat(capturedPillarMap).containsKey("p1");

    Pillar pillar = capturedPillarMap.get("p1");
    assertThat(pillar.getName()).isEqualTo("Technical Skills");
    assertThat(pillar.getDescription()).isEqualTo("Technical knowledge and abilities");
    assertThat(pillar.getCategoryMap()).containsKeys("c1", "c2");

    Category category1 = pillar.getCategoryMap().get("c1");
    assertThat(category1.getName()).isEqualTo("Programming");
    assertThat(category1.getDescription()).isEqualTo("Programming skills and knowledge");

    // Verify the response
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-matrix-id");
  }

  @Test
  void handleGetAll_whenTenantIdProvided_shouldReturnMatricesForTenant() {
    // Given
    String tenantId = "tenant-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    AssessmentMatrix matrix1 = AssessmentMatrix.builder()
        .id("matrix-1")
        .name("Engineering Matrix")
        .description("For engineering assessments")
        .tenantId(tenantId)
        .performanceCycleId("cycle-1")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    AssessmentMatrix matrix2 = AssessmentMatrix.builder()
        .id("matrix-2")
        .name("Sales Matrix")
        .description("For sales assessments")
        .tenantId(tenantId)
        .performanceCycleId("cycle-2")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    List<AssessmentMatrix> matrices = Arrays.asList(matrix1, matrix2);

    doReturn(matrices).when(assessmentMatrixService).findAllByTenantId(tenantId);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("matrix-1", "Engineering Matrix");
    assertThat(response.getBody()).contains("matrix-2", "Sales Matrix");
    verify(assessmentMatrixService).findAllByTenantId(tenantId);
    verify(assessmentMatrixService, never()).findAll();
  }

  @Test
  void handleGetAll_whenNoTenantIdProvided_shouldReturnBadRequest() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("tenantId is required");
    verify(assessmentMatrixService, never()).findAll();
    verify(assessmentMatrixService, never()).findAllByTenantId(anyString());
  }

  @Test
  void shouldCreateAssessmentMatrixWithConfiguration() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {},\n" +
        "  \"configuration\": {\n" +
        "    \"allowQuestionReview\": false,\n" +
        "    \"requireAllQuestions\": true,\n" +
        "    \"autoSave\": false,\n" +
        "    \"navigationMode\": \"SEQUENTIAL\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        eq("Engineering Matrix"),
        eq("Competency assessment for engineering roles"),
        eq("tenant-test-id-123"),
        eq("3837551b-20f2-41eb-9779-8203a5209c45"),
        anyMap(),
        configurationCaptor.capture());

    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig.getAllowQuestionReview()).isFalse();
    assertThat(capturedConfig.getRequireAllQuestions()).isTrue();
    assertThat(capturedConfig.getAutoSave()).isFalse();
    assertThat(capturedConfig.getNavigationMode()).isEqualTo(QuestionNavigationType.SEQUENTIAL);

    assertThat(response.getStatusCode()).isEqualTo(201);
  }

  @Test
  void shouldCreateAssessmentMatrixWithDefaultConfigurationValues() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {},\n" +
        "  \"configuration\": {\n" +
        "    \"navigationMode\": \"FREE_FORM\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), configurationCaptor.capture());

    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig.getAllowQuestionReview()).isTrue(); // default
    assertThat(capturedConfig.getRequireAllQuestions()).isTrue(); // default
    assertThat(capturedConfig.getAutoSave()).isTrue(); // default
    assertThat(capturedConfig.getNavigationMode()).isEqualTo(QuestionNavigationType.FREE_FORM);

    assertThat(response.getStatusCode()).isEqualTo(201);
  }

  @Test
  void shouldCreateAssessmentMatrixWithoutConfiguration() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {}\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), configurationCaptor.capture());

    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig).isNull(); // No configuration provided

    assertThat(response.getStatusCode()).isEqualTo(201);
  }

  @Test
  void shouldHandleInvalidNavigationModeGracefully() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {},\n" +
        "  \"configuration\": {\n" +
        "    \"navigationMode\": \"INVALID_MODE\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        anyString(), anyString(), anyString(), anyString(), anyMap(), configurationCaptor.capture());

    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig.getNavigationMode()).isEqualTo(QuestionNavigationType.RANDOM); // fallback to default

    assertThat(response.getStatusCode()).isEqualTo(201);
  }

  @Test
  void shouldUpdateAssessmentMatrixWithConfiguration() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Updated Engineering Matrix\",\n" +
        "  \"description\": \"Updated competency assessment\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {},\n" +
        "  \"configuration\": {\n" +
        "    \"allowQuestionReview\": true,\n" +
        "    \"requireAllQuestions\": false,\n" +
        "    \"autoSave\": true,\n" +
        "    \"navigationMode\": \"RANDOM\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/matrix-123")
        .withHttpMethod("PUT")
        .withBody(requestBody);

    AssessmentMatrix updatedMatrix = AssessmentMatrix.builder()
        .id("matrix-123")
        .name("Updated Engineering Matrix")
        .description("Updated competency assessment")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .pillarMap(new HashMap<>())
        .questionCount(0)
        .build();

    doReturn(Optional.of(updatedMatrix)).when(assessmentMatrixService).update(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyMap(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).update(
        eq("matrix-123"),
        eq("Updated Engineering Matrix"),
        eq("Updated competency assessment"),
        eq("tenant-test-id-123"),
        eq("3837551b-20f2-41eb-9779-8203a5209c45"),
        anyMap(),
        configurationCaptor.capture());

    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig.getAllowQuestionReview()).isTrue();
    assertThat(capturedConfig.getRequireAllQuestions()).isFalse();
    assertThat(capturedConfig.getAutoSave()).isTrue();
    assertThat(capturedConfig.getNavigationMode()).isEqualTo(QuestionNavigationType.RANDOM);

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void handleGetDashboard_whenValidRequest_shouldReturnDashboard() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    com.agilecheckup.service.dto.AssessmentDashboardData dashboardData =
        com.agilecheckup.service.dto.AssessmentDashboardData.builder()
            .assessmentMatrixId(matrixId)
            .matrixName("Test Matrix")
            .teamSummaries(new ArrayList<>())
            .employeeSummaries(new ArrayList<>())
            .totalEmployees(5)
            .completedAssessments(3)
            .build();

    doReturn(Optional.of(dashboardData)).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(Collections.singletonMap("tenantId", tenantId));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"matrixId\":\"" + matrixId + "\"");
    assertThat(response.getBody()).contains("\"matrixName\":\"Test Matrix\"");
    assertThat(response.getBody()).contains("\"totalEmployees\":5");
    assertThat(response.getBody()).contains("\"completedAssessments\":3");

    verify(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);
  }

  @Test
  void handleGetDashboard_whenNoTenantId_shouldReturnBadRequest() {
    // Given
    String matrixId = "matrix-123";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("tenantId is required");
  }

  @Test
  void handleGetDashboard_whenMatrixNotFound_shouldReturnNotFound() {
    // Given
    String matrixId = "nonexistent-matrix";
    String tenantId = "tenant-456";

    doReturn(Optional.empty()).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(Collections.singletonMap("tenantId", tenantId));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).isEqualTo("Assessment matrix not found or access denied");

    verify(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);
  }

  @Test
  void handleGetDashboard_withPaginationParameters_shouldReturnPaginatedResults() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    // Create larger dataset for pagination testing
    List<com.agilecheckup.service.dto.EmployeeAssessmentSummary> employeeSummaries = createLargeEmployeeSummaryList(10);

    com.agilecheckup.service.dto.AssessmentDashboardData dashboardData =
        com.agilecheckup.service.dto.AssessmentDashboardData.builder()
            .assessmentMatrixId(matrixId)
            .matrixName("Test Matrix")
            .teamSummaries(createTestTeamSummaries())
            .employeeSummaries(employeeSummaries)
            .totalEmployees(10)
            .completedAssessments(6)
            .build();

    doReturn(Optional.of(dashboardData)).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);
    queryParams.put("page", "2");
    queryParams.put("pageSize", "3");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    String responseBody = response.getBody();

    // Verify pagination in response
    assertThat(responseBody).contains("\"page\":2");
    assertThat(responseBody).contains("\"pageSize\":3");
    assertThat(responseBody).contains("\"totalCount\":10");

    // Verify team summaries are included
    assertThat(responseBody).contains("\"teamSummaries\":");

    verify(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);
  }

  @Test
  void handleGetDashboard_withInvalidPaginationParameters_shouldReturnBadRequest() {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);
    queryParams.put("page", "0"); // Invalid: page must be >= 1
    queryParams.put("pageSize", "300"); // Invalid: pageSize must be <= 200

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Invalid pagination parameters");
  }

  @Test
  void handleGetDashboard_withNonNumericPaginationParameters_shouldUseDefaults() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    com.agilecheckup.service.dto.AssessmentDashboardData dashboardData =
        com.agilecheckup.service.dto.AssessmentDashboardData.builder()
            .assessmentMatrixId(matrixId)
            .matrixName("Test Matrix")
            .teamSummaries(new ArrayList<>())
            .employeeSummaries(new ArrayList<>())
            .totalEmployees(5)
            .completedAssessments(3)
            .build();

    doReturn(Optional.of(dashboardData)).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);
    queryParams.put("page", "invalid"); // Non-numeric
    queryParams.put("pageSize", "also-invalid"); // Non-numeric

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    String responseBody = response.getBody();

    // Should use default values: page=1, pageSize=50
    assertThat(responseBody).contains("\"page\":1");
    assertThat(responseBody).contains("\"pageSize\":50");
  }

  @Test
  void handleGetDashboard_withComplexDashboardData_shouldReturnCompleteResponse() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    // Create consistent employee data with 10 employees
    List<com.agilecheckup.service.dto.EmployeeAssessmentSummary> employeeSummaries = createLargeEmployeeSummaryList(10);

    com.agilecheckup.service.dto.AssessmentDashboardData dashboardData =
        com.agilecheckup.service.dto.AssessmentDashboardData.builder()
            .assessmentMatrixId(matrixId)
            .matrixName("Complex Test Matrix")
            .potentialScore(createTestPotentialScore())
            .teamSummaries(createTestTeamSummaries())
            .employeeSummaries(employeeSummaries)
            .totalEmployees(10)
            .completedAssessments(6)
            .build();

    doReturn(Optional.of(dashboardData)).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(Collections.singletonMap("tenantId", tenantId));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    String responseBody = response.getBody();

    // Verify all major components are present
    assertThat(responseBody).contains("\"matrixId\":\"" + matrixId + "\"");
    assertThat(responseBody).contains("\"matrixName\":\"Complex Test Matrix\"");
    assertThat(responseBody).contains("\"potentialScore\":");
    assertThat(responseBody).contains("\"teamSummaries\":");
    assertThat(responseBody).contains("\"employees\":");
    assertThat(responseBody).contains("\"totalEmployees\":10");
    assertThat(responseBody).contains("\"completedAssessments\":6");

    // Verify team summary structure
    assertThat(responseBody).contains("\"teamId\":\"team-1\"");
    assertThat(responseBody).contains("\"teamName\":\"Engineering Team\"");
    assertThat(responseBody).contains("\"completionPercentage\":");
    assertThat(responseBody).contains("\"averageScore\":");

    // Verify employee details structure
    assertThat(responseBody).contains("\"employeeAssessmentId\":");
    assertThat(responseBody).contains("\"employeeName\":");
    assertThat(responseBody).contains("\"status\":");

    verify(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);
  }

  @Test
  void handleGetDashboard_withLargeDataset_shouldPaginateCorrectly() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    // Create large dataset
    List<com.agilecheckup.service.dto.EmployeeAssessmentSummary> largeEmployeeList =
        createLargeEmployeeSummaryList(150); // 150 employees

    com.agilecheckup.service.dto.AssessmentDashboardData dashboardData =
        com.agilecheckup.service.dto.AssessmentDashboardData.builder()
            .assessmentMatrixId(matrixId)
            .matrixName("Large Matrix")
            .teamSummaries(createTestTeamSummaries())
            .employeeSummaries(largeEmployeeList)
            .totalEmployees(150)
            .completedAssessments(100)
            .build();

    doReturn(Optional.of(dashboardData)).when(assessmentMatrixService)
        .getAssessmentDashboard(matrixId, tenantId);

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);
    queryParams.put("page", "3");
    queryParams.put("pageSize", "50");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    String responseBody = response.getBody();

    // Verify pagination
    assertThat(responseBody).contains("\"page\":3");
    assertThat(responseBody).contains("\"pageSize\":50");
    assertThat(responseBody).contains("\"totalCount\":150");

    // Verify content length is appropriate for page 3 (should contain 50 employees)
    assertThat(responseBody).contains("\"content\":");

    verify(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);
  }

  @Test
  void handleGetDashboard_withServiceException_shouldReturnInternalServerError() throws Exception {
    // Given
    String matrixId = "matrix-123";
    String tenantId = "tenant-456";

    doThrow(new RuntimeException("Database connection failed"))
        .when(assessmentMatrixService).getAssessmentDashboard(matrixId, tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices/" + matrixId + "/dashboard")
        .withHttpMethod("GET")
        .withQueryStringParameters(Collections.singletonMap("tenantId", tenantId));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error");
  }

  // ========== Helper Methods for Dashboard Tests ==========

  // Team summaries created for
  private List<com.agilecheckup.service.dto.TeamAssessmentSummary> createTestTeamSummaries() {
    List<com.agilecheckup.service.dto.TeamAssessmentSummary> summaries = new ArrayList<>();

    summaries.add(com.agilecheckup.service.dto.TeamAssessmentSummary.builder()
        .teamId("team-1")
        .teamName("Engineering Team")
        .totalEmployees(5)
        .completedAssessments(3)
        .completionPercentage(60.0)
        .averageScore(85.5)
        .build());

    summaries.add(com.agilecheckup.service.dto.TeamAssessmentSummary.builder()
        .teamId("team-2")
        .teamName("Design Team")
        .totalEmployees(3)
        .completedAssessments(2)
        .completionPercentage(66.67)
        .averageScore(78.0)
        .build());

    return summaries;
  }

  // Employee summaries created for
  private List<com.agilecheckup.service.dto.EmployeeAssessmentSummary> createTestEmployeeSummaries() {
    List<com.agilecheckup.service.dto.EmployeeAssessmentSummary> summaries = new ArrayList<>();

    summaries.add(com.agilecheckup.service.dto.EmployeeAssessmentSummary.builder()
        .employeeAssessmentId("assessment-1")
        .employeeId("emp1")
        .employeeName("John Doe")
        .employeeEmail("john@example.com")
        .teamId("team-1")
        .teamName("Engineering Team")
        .assessmentStatus(com.agilecheckup.persistency.entity.AssessmentStatus.COMPLETED)
        .currentScore(85.0)
        .answeredQuestionCount(15)
        .lastActivityDate(java.time.LocalDateTime.now())
        .build());

    summaries.add(com.agilecheckup.service.dto.EmployeeAssessmentSummary.builder()
        .employeeAssessmentId("assessment-2")
        .employeeId("emp2")
        .employeeName("Jane Smith")
        .employeeEmail("jane@example.com")
        .teamId("team-1")
        .teamName("Engineering Team")
        .assessmentStatus(com.agilecheckup.persistency.entity.AssessmentStatus.IN_PROGRESS)
        .currentScore(null)
        .answeredQuestionCount(8)
        .lastActivityDate(java.time.LocalDateTime.now().minusHours(2))
        .build());

    return summaries;
  }

  // Employee summaries created for
  private List<EmployeeAssessmentSummary> createLargeEmployeeSummaryList(int count) {
    List<EmployeeAssessmentSummary> summaries = new ArrayList<>();

    for (int i = 1; i <= count; i++) {
      summaries.add(com.agilecheckup.service.dto.EmployeeAssessmentSummary.builder()
          .employeeAssessmentId("assessment-" + i)
          .employeeId("emp" + i)
          .employeeName("Employee " + i)
          .employeeEmail("employee" + i + "@example.com")
          .teamId("team-" + ((i % 5) + 1))
          .teamName("Team " + ((i % 5) + 1))
          .assessmentStatus(i % 3 == 0 ?
              com.agilecheckup.persistency.entity.AssessmentStatus.COMPLETED :
              com.agilecheckup.persistency.entity.AssessmentStatus.IN_PROGRESS)
          .currentScore(i % 3 == 0 ? Double.valueOf(70 + (i % 30)) : null)
          .answeredQuestionCount(i % 20)
          .lastActivityDate(java.time.LocalDateTime.now().minusHours(i % 24))
          .build());
    }

    return summaries;
  }

  private PotentialScore createTestPotentialScore() {
    return PotentialScore.builder()
        .score(100.0)
        .pillarIdToPillarScoreMap(new HashMap<>())
        .build();
  }

  @Test
  void shouldSuccessfullyCreateAssessmentMatrixWithPillarAndCategory() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \" Engineering Matrix\",\n" +
        "  \"description\": \" Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-v2-test-id-123\",\n" +
        "  \"performanceCycleId\": \"v2-3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {\n" +
        "    \"p1-v2\": {\n" +
        "      \"name\": \"Technical Skills \",\n" +
        "      \"description\": \" Technical knowledge and abilities\",\n" +
        "      \"categoryMap\": {\n" +
        "        \"c1-v2\": {\n" +
        "          \"name\": \"Programming \",\n" +
        "          \"description\": \" Programming skills and knowledge\"\n" +
        "        },\n" +
        "        \"c2-v2\": {\n" +
        "          \"name\": \"Architecture \",\n" +
        "          \"description\": \" System design and architecture\"\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"p2-v2\": {\n" +
        "      \"name\": \"Soft Skills \",\n" +
        "      \"description\": \" Communication and teamwork\",\n" +
        "      \"categoryMap\": {\n" +
        "        \"c3-v2\": {\n" +
        "          \"name\": \"Leadership \",\n" +
        "          \"description\": \" Leadership abilities\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"configuration\": {\n" +
        "    \"allowQuestionReview\": false,\n" +
        "    \"requireAllQuestions\": true,\n" +
        "    \"autoSave\": false,\n" +
        "    \"navigationMode\": \"SEQUENTIAL\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // Create a sample assessment matrix to return
    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-v2-matrix-id")
        .name(" Engineering Matrix")
        .description(" Competency assessment for engineering roles")
        .tenantId("tenant-v2-test-id-123")
        .performanceCycleId("v2-3837551b-20f2-41eb-9779-8203a5209c45")
        .build();

    doReturn(Optional.of(createdMatrix)).when(assessmentMatrixService)
        .create(anyString(), anyString(), anyString(), anyString(), any(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    // Verify the service was called with correct parameters
    verify(assessmentMatrixService).create(
        eq(" Engineering Matrix"),
        eq(" Competency assessment for engineering roles"),
        eq("tenant-v2-test-id-123"),
        eq("v2-3837551b-20f2-41eb-9779-8203a5209c45"),
        pillarMapCaptor.capture(),
        configurationCaptor.capture());

    // Verify the  pillar map was built correctly
    Map<String, Pillar> capturedPillarMap = pillarMapCaptor.getValue();
    assertThat(capturedPillarMap).hasSize(2);
    assertThat(capturedPillarMap).containsKeys("p1-v2", "p2-v2");

    // Verify first  pillar
    Pillar pillar1 = capturedPillarMap.get("p1-v2");
    assertThat(pillar1.getId()).isEqualTo("p1-v2");
    assertThat(pillar1.getName()).isEqualTo("Technical Skills ");
    assertThat(pillar1.getDescription()).isEqualTo(" Technical knowledge and abilities");
    assertThat(pillar1.getCategoryMap()).hasSize(2);

    // Verify  categories in first pillar
    Category category1 = pillar1.getCategoryMap().get("c1-v2");
    assertThat(category1.getId()).isEqualTo("c1-v2");
    assertThat(category1.getName()).isEqualTo("Programming ");
    assertThat(category1.getDescription()).isEqualTo(" Programming skills and knowledge");

    Category category2 = pillar1.getCategoryMap().get("c2-v2");
    assertThat(category2.getId()).isEqualTo("c2-v2");
    assertThat(category2.getName()).isEqualTo("Architecture ");
    assertThat(category2.getDescription()).isEqualTo(" System design and architecture");

    // Verify second  pillar
    Pillar pillar2 = capturedPillarMap.get("p2-v2");
    assertThat(pillar2.getId()).isEqualTo("p2-v2");
    assertThat(pillar2.getName()).isEqualTo("Soft Skills ");
    assertThat(pillar2.getDescription()).isEqualTo(" Communication and teamwork");
    assertThat(pillar2.getCategoryMap()).hasSize(1);

    // Verify configuration
    AssessmentConfiguration capturedConfig = configurationCaptor.getValue();
    assertThat(capturedConfig.getAllowQuestionReview()).isFalse();
    assertThat(capturedConfig.getRequireAllQuestions()).isTrue();
    assertThat(capturedConfig.getAutoSave()).isFalse();
    assertThat(capturedConfig.getNavigationMode()).isEqualTo(QuestionNavigationType.SEQUENTIAL);

    // Verify the response
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-v2-matrix-id");
  }
}