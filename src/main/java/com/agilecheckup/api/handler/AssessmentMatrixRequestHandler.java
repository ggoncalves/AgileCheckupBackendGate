package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.gate.cache.CacheManager;
import com.agilecheckup.gate.dto.DashboardResponse;
import com.agilecheckup.gate.dto.EmployeeAssessmentDetail;
import com.agilecheckup.gate.dto.EmployeePageResponse;
import com.agilecheckup.gate.dto.TeamSummary;
import com.agilecheckup.api.model.CategoryApiV2;
import com.agilecheckup.api.model.PillarApiV2;
import com.agilecheckup.persistency.entity.*;
import com.agilecheckup.persistency.entity.AssessmentMatrixV2;
import com.agilecheckup.service.AssessmentMatrixService;
import com.agilecheckup.service.AssessmentMatrixServiceV2;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AssessmentMatrixRequestHandler extends AbstractCrudRequestHandler<AssessmentMatrix> {

  private static final Pattern UPDATE_POTENTIAL_SCORE_PATTERN = Pattern.compile("^/assessmentmatrices/([^/]+)/potentialscore/?$");
  private static final Pattern DASHBOARD_PATTERN = Pattern.compile("^/assessmentmatrices/([^/]+)/dashboard/?$");

  private final AssessmentMatrixService assessmentMatrixService;
  private final AssessmentMatrixServiceV2 assessmentMatrixServiceV2;
  private final CacheManager cacheManager;

  public AssessmentMatrixRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    super(objectMapper, "assessmentmatrices");
    this.assessmentMatrixService = serviceComponent.buildAssessmentMatrixService();
    this.assessmentMatrixServiceV2 = serviceComponent.buildAssessmentMatrixServiceV2();
    this.cacheManager = new CacheManager(); // Simple instantiation for now - to be fixed later
  }

  // Constructor for testing with mocked cache manager
  public AssessmentMatrixRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper, CacheManager cacheManager) {
    super(objectMapper, "assessmentmatrices");
    this.assessmentMatrixService = serviceComponent.buildAssessmentMatrixService();
    this.assessmentMatrixServiceV2 = serviceComponent.buildAssessmentMatrixServiceV2();
    this.cacheManager = cacheManager;
  }

  @Override
  protected String getResourceName() {
    return "assessment matrix";
  }
  
  @Override
  protected Optional<APIGatewayProxyResponseEvent> handleCustomEndpoint(String method, String path, 
                                                                       APIGatewayProxyRequestEvent input, 
                                                                       Context context) throws Exception {
    // Handle special endpoint: POST /assessmentmatrices/{id}/potentialscore
    if (method.equals("POST") && UPDATE_POTENTIAL_SCORE_PATTERN.matcher(path).matches()) {
      String id = extractIdFromPath(path.replace("/potentialscore", ""));
      return Optional.of(handleUpdatePotentialScore(id, input.getBody()));
    }

    // Handle dashboard endpoint: GET /assessmentmatrices/{id}/dashboard
    if (method.equals("GET") && DASHBOARD_PATTERN.matcher(path).matches()) {
      String id = extractIdFromPath(path.replace("/dashboard", ""));
      return Optional.of(handleGetDashboard(id, input, context));
    }

    return Optional.empty();
  }

  @Override
  protected APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();

    if (queryParams != null && queryParams.containsKey("tenantId")) {
      String tenantId = queryParams.get("tenantId");
      List<AssessmentMatrixV2> matrices = assessmentMatrixServiceV2.findAllByTenantId(tenantId);
      String jsonResponse = objectMapper.writeValueAsString(matrices);
      return ResponseBuilder.buildResponse(200, jsonResponse);
    }

    // No tenantId provided - return error for security
    return ResponseBuilder.buildResponse(400, "tenantId is required");
  }

  @Override
  protected APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<AssessmentMatrixV2> assessmentMatrix = assessmentMatrixServiceV2.findById(id);

    if (assessmentMatrix.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrix.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
    }
  }

  @Override
  protected APIGatewayProxyResponseEvent handleCreate(String requestBody, Context context) throws Exception {
    try {
      Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

      // Create the pillar map with V2 types
      Map<String, PillarV2> pillarMap = buildPillarMapV2(requestMap);

      // Create the assessment configuration if provided
      AssessmentConfiguration configuration = buildAssessmentConfiguration(requestMap);

      // Now create the assessment matrix with properly typed pillar map and configuration
      Optional<AssessmentMatrixV2> assessmentMatrix = assessmentMatrixServiceV2.create(
          (String) requestMap.get("name"),
          (String) requestMap.get("description"),
          (String) requestMap.get("tenantId"),
          (String) requestMap.get("performanceCycleId"),
          pillarMap,
          configuration
      );

      if (assessmentMatrix.isPresent()) {
        return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(assessmentMatrix.get()));
      } else {
        return ResponseBuilder.buildResponse(400, "Failed to create assessment matrix");
      }
    } catch (Exception e) {
      context.getLogger().log("Detailed error: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error creating assessment matrix: " + e.getMessage());
    }
  }

  @Override
  protected APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody, Context context) throws Exception {
    try {
      Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

      // Create the pillar map with V2 types
      Map<String, PillarV2> pillarMap = buildPillarMapV2(requestMap);

      // Create the assessment configuration if provided
      AssessmentConfiguration configuration = buildAssessmentConfiguration(requestMap);

      Optional<AssessmentMatrixV2> assessmentMatrix = assessmentMatrixServiceV2.update(
          id,
          (String) requestMap.get("name"),
          (String) requestMap.get("description"),
          (String) requestMap.get("tenantId"),
          (String) requestMap.get("performanceCycleId"),
          pillarMap,
          configuration
      );

      if (assessmentMatrix.isPresent()) {
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrix.get()));
      } else {
        return ResponseBuilder.buildResponse(404, "Assessment matrix not found or update failed");
      }
    } catch (Exception e) {
      context.getLogger().log("Detailed error: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error updating assessment matrix: " + e.getMessage());
    }
  }

  private Map<String, Pillar> buildPillarMap(Map<String, Object> requestMap) {
    Map<String, Pillar> pillarMap = new HashMap<>();
    if (requestMap.containsKey("pillarMap") && requestMap.get("pillarMap") != null) {
      Map<String, Object> rawPillarMap = (Map<String, Object>) requestMap.get("pillarMap");

      for (Map.Entry<String, Object> entry : rawPillarMap.entrySet()) {
        String pillarId = entry.getKey();
        Map<String, Object> pillarData = (Map<String, Object>) entry.getValue();

        Map<String, Category> categoryMap = new HashMap<>();
        if (pillarData.containsKey("categoryMap") && pillarData.get("categoryMap") != null) {
          Map<String, Object> rawCategoryMap = (Map<String, Object>) pillarData.get("categoryMap");

          for (Map.Entry<String, Object> catEntry : rawCategoryMap.entrySet()) {
            String categoryId = catEntry.getKey();
            Map<String, Object> categoryData = (Map<String, Object>) catEntry.getValue();

            Category category = Category.builder()
                .id(categoryId)
                .name((String) categoryData.get("name"))
                .description((String) categoryData.get("description"))
                .build();

            categoryMap.put(categoryId, category);
          }
        }

        Pillar pillar = Pillar.builder()
            .id(pillarId)
            .name((String) pillarData.get("name"))
            .description((String) pillarData.get("description"))
            .categoryMap(categoryMap)
            .build();

        pillarMap.put(pillarId, pillar);
      }
    }
    return pillarMap;
  }

  private AssessmentConfiguration buildAssessmentConfiguration(Map<String, Object> requestMap) {
    if (!hasConfiguration(requestMap)) {
      return null;
    }

    Map<String, Object> configData = extractConfigurationData(requestMap);

    return AssessmentConfiguration.builder()
        .allowQuestionReview(getBooleanOrDefault(configData, "allowQuestionReview", true))
        .requireAllQuestions(getBooleanOrDefault(configData, "requireAllQuestions", true))
        .autoSave(getBooleanOrDefault(configData, "autoSave", true))
        .navigationMode(getNavigationModeOrDefault(configData))
        .build();
  }

  private boolean hasConfiguration(Map<String, Object> requestMap) {
    return requestMap.containsKey("configuration") && requestMap.get("configuration") != null;
  }

  private Map<String, Object> extractConfigurationData(Map<String, Object> requestMap) {
    return (Map<String, Object>) requestMap.get("configuration");
  }

  private Boolean getBooleanOrDefault(Map<String, Object> configData, String key, Boolean defaultValue) {
    return configData.containsKey(key) ? (Boolean) configData.get(key) : defaultValue;
  }

  private QuestionNavigationType getNavigationModeOrDefault(Map<String, Object> configData) {
    if (!configData.containsKey("navigationMode") || configData.get("navigationMode") == null) {
      return QuestionNavigationType.RANDOM;
    }

    try {
      return QuestionNavigationType.valueOf((String) configData.get("navigationMode"));
    } catch (IllegalArgumentException e) {
      return QuestionNavigationType.RANDOM;
    }
  }

  private APIGatewayProxyResponseEvent handleUpdatePotentialScore(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);
    String tenantId = (String) requestMap.get("tenantId");

    AssessmentMatrixV2 assessmentMatrix = assessmentMatrixServiceV2.updateCurrentPotentialScore(id, tenantId);

    if (assessmentMatrix != null) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrix));
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found or update failed");
    }
  }

  @Override
  protected APIGatewayProxyResponseEvent handleDelete(String id) throws Exception {
    boolean deleted = assessmentMatrixServiceV2.deleteById(id);

    if (deleted) {
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
    }
  }

  /**
   * Handles GET /assessmentmatrices/{id}/dashboard endpoint.
   * Returns comprehensive dashboard data with caching and pagination support.
   */
  private APIGatewayProxyResponseEvent handleGetDashboard(String matrixId, APIGatewayProxyRequestEvent input, Context context) throws Exception {
    try {
      Map<String, String> queryParams = input.getQueryStringParameters();

      // Extract required tenant ID
      if (queryParams == null || !queryParams.containsKey("tenantId")) {
        return ResponseBuilder.buildResponse(400, "tenantId is required");
      }
      String tenantId = queryParams.get("tenantId");

      // Extract pagination parameters
      int page = extractIntParam(queryParams, "page", 1);
      int pageSize = extractIntParam(queryParams, "pageSize", 50);

      // Validate pagination parameters
      if (page < 1 || pageSize < 1 || pageSize > 200) {
        return ResponseBuilder.buildResponse(400, "Invalid pagination parameters. Page must be >= 1, pageSize must be 1-200");
      }

      // Check cache first (include pagination in cache key)
      String cacheKey = "dashboard:" + matrixId + ":" + tenantId + ":" + page + ":" + pageSize;
      Optional<DashboardResponse> cachedResponse = cacheManager.get(cacheKey, DashboardResponse.class);

      if (cachedResponse.isPresent()) {
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(cachedResponse.get()));
      }

      // Get dashboard data from service
      Optional<com.agilecheckup.service.dto.AssessmentDashboardData> dashboardData =
          assessmentMatrixServiceV2.getAssessmentDashboard(matrixId, tenantId);

      if (!dashboardData.isPresent()) {
        return ResponseBuilder.buildResponse(404, "Assessment matrix not found or access denied");
      }

      // Convert to presentation DTO with pagination
      DashboardResponse response = convertToDashboardResponse(dashboardData.get(), page, pageSize);

      // Cache the response
      cacheManager.put(cacheKey, response);

      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));
      
    } catch (Exception e) {
      context.getLogger().log("Dashboard error: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Extracts integer parameter from query string with default value.
   */
  private int extractIntParam(Map<String, String> queryParams, String paramName, int defaultValue) {
    if (queryParams == null || !queryParams.containsKey(paramName)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(queryParams.get(paramName));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Converts domain DTO to presentation DTO with pagination support.
   */
  private DashboardResponse convertToDashboardResponse(com.agilecheckup.service.dto.AssessmentDashboardData dashboardData, int page, int pageSize) {
    // Convert team summaries (no pagination needed for teams)
    List<TeamSummary> teamSummaries = dashboardData.getTeamSummaries().stream()
        .map(ts -> TeamSummary.builder()
            .teamId(ts.getTeamId())
            .teamName(ts.getTeamName())
            .totalEmployees(ts.getTotalEmployees())
            .completedAssessments(ts.getCompletedAssessments())
            .completionPercentage(ts.getCompletionPercentage())
            .averageScore(ts.getAverageScore())
            .build())
        .collect(Collectors.toList());

    // Apply pagination to employee summaries
    List<EmployeeAssessmentDetail> allEmployeeDetails = dashboardData.getEmployeeSummaries().stream()
        .map(es -> EmployeeAssessmentDetail.builder()
            .employeeAssessmentId(es.getEmployeeAssessmentId())
            .employeeName(es.getEmployeeName())
            .employeeEmail(es.getEmployeeEmail())
            .teamId(es.getTeamId())
            .status(es.getAssessmentStatus() != null ? es.getAssessmentStatus().name() : "UNKNOWN")
            .currentScore(es.getCurrentScore())
            .answeredQuestions(es.getAnsweredQuestions())
            .lastActivityDate(es.getLastActivityDate())
            .build())
        .collect(Collectors.toList());

    // Apply pagination
    int totalEmployees = allEmployeeDetails.size();
    int startIndex = (page - 1) * pageSize;
    int endIndex = Math.min(startIndex + pageSize, totalEmployees);

    List<EmployeeAssessmentDetail> paginatedEmployeeDetails = startIndex < totalEmployees
        ? allEmployeeDetails.subList(startIndex, endIndex)
        : new ArrayList<>();

    return DashboardResponse.builder()
        .matrixId(dashboardData.getAssessmentMatrixId())
        .matrixName(dashboardData.getMatrixName())
        .potentialScore(dashboardData.getPotentialScore())
        .teamSummaries(teamSummaries)
        .employees(EmployeePageResponse.builder()
            .content(paginatedEmployeeDetails)
            .totalCount(totalEmployees)
            .page(page)
            .pageSize(pageSize)
            .build())
        .totalEmployees(dashboardData.getTotalEmployees())
        .completedAssessments(dashboardData.getCompletedAssessments())
        .build();
  }

  /**
   * Backward compatibility method for tests.
   */
  private DashboardResponse convertToDashboardResponse(com.agilecheckup.service.dto.AssessmentDashboardData dashboardData) {
    return convertToDashboardResponse(dashboardData, 1, 50);
  }

  /**
   * Builds PillarV2 map from request data using V2 entities
   */
  private Map<String, PillarV2> buildPillarMapV2(Map<String, Object> requestMap) {
    Map<String, PillarV2> pillarMap = new HashMap<>();
    if (requestMap.containsKey("pillarMap") && requestMap.get("pillarMap") != null) {
      Map<String, Object> rawPillarMap = (Map<String, Object>) requestMap.get("pillarMap");

      for (Map.Entry<String, Object> entry : rawPillarMap.entrySet()) {
        String pillarId = entry.getKey();
        Map<String, Object> pillarData = (Map<String, Object>) entry.getValue();

        Map<String, CategoryV2> categoryMap = new HashMap<>();
        if (pillarData.containsKey("categoryMap") && pillarData.get("categoryMap") != null) {
          Map<String, Object> rawCategoryMap = (Map<String, Object>) pillarData.get("categoryMap");

          for (Map.Entry<String, Object> catEntry : rawCategoryMap.entrySet()) {
            String categoryId = catEntry.getKey();
            Map<String, Object> categoryData = (Map<String, Object>) catEntry.getValue();

            CategoryV2 category = CategoryV2.builder()
                .id(categoryId)
                .name((String) categoryData.get("name"))
                .description((String) categoryData.get("description"))
                .build();

            categoryMap.put(categoryId, category);
          }
        }

        PillarV2 pillar = PillarV2.builder()
            .id(pillarId)
            .name((String) pillarData.get("name"))
            .description((String) pillarData.get("description"))
            .categoryMap(categoryMap)
            .build();

        pillarMap.put(pillarId, pillar);
      }
    }
    return pillarMap;
  }

  /**
   * Converts PillarV2 entity to PillarApiV2 API model
   */
  private PillarApiV2 convertPillarV2ToApi(PillarV2 pillar) {
    if (pillar == null) {
      return null;
    }

    Map<String, CategoryApiV2> categoryApiMap = new HashMap<>();
    if (pillar.getCategoryMap() != null) {
      for (Map.Entry<String, CategoryV2> entry : pillar.getCategoryMap().entrySet()) {
        categoryApiMap.put(entry.getKey(), convertCategoryV2ToApi(entry.getValue()));
      }
    }

    return PillarApiV2.builder()
        .id(pillar.getId())
        .name(pillar.getName())
        .description(pillar.getDescription())
        .categoryMap(categoryApiMap)
        .createdDate(pillar.getCreatedDate() != null ? pillar.getCreatedDate().toString() : null)
        .lastUpdatedDate(pillar.getLastUpdatedDate() != null ? pillar.getLastUpdatedDate().toString() : null)
        .build();
  }

  /**
   * Converts CategoryV2 entity to CategoryApiV2 API model
   */
  private CategoryApiV2 convertCategoryV2ToApi(CategoryV2 category) {
    if (category == null) {
      return null;
    }

    return CategoryApiV2.builder()
        .id(category.getId())
        .name(category.getName())
        .description(category.getDescription())
        .createdDate(category.getCreatedDate() != null ? category.getCreatedDate().toString() : null)
        .lastUpdatedDate(category.getLastUpdatedDate() != null ? category.getLastUpdatedDate().toString() : null)
        .build();
  }

}