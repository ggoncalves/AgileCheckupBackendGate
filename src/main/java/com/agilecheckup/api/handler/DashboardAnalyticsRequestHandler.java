package com.agilecheckup.api.handler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.gate.dto.DashboardAnalyticsOverviewResponse;
import com.agilecheckup.gate.dto.DashboardAnalyticsTeamResponse;
import com.agilecheckup.gate.dto.PerformanceCycleSummaryResponse;
import com.agilecheckup.persistency.entity.AnalyticsScope;
import com.agilecheckup.persistency.entity.AssessmentMatrix;
import com.agilecheckup.persistency.entity.DashboardAnalytics;
import com.agilecheckup.service.AssessmentMatrixService;
import com.agilecheckup.service.DashboardAnalyticsService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DashboardAnalyticsRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern OVERVIEW_PATTERN = Pattern.compile("^/dashboard-analytics/overview/([^/]+)/?$");
  private static final Pattern TEAM_PATTERN = Pattern.compile("^/dashboard-analytics/team/([^/]+)/([^/]+)/?$");
  private static final Pattern COMPUTE_PATTERN = Pattern.compile("^/dashboard-analytics/compute/([^/]+)/?$");
  private static final Pattern PERFORMANCE_CYCLE_SUMMARY_PATTERN = Pattern.compile("^/performance-cycle-summary/([^/]+)/?$");

  private final DashboardAnalyticsService dashboardAnalyticsService;
  private final AssessmentMatrixService assessmentMatrixService;
  private final ObjectMapper objectMapper;

  public DashboardAnalyticsRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.dashboardAnalyticsService = serviceComponent.buildDashboardAnalyticsService();
    this.assessmentMatrixService = serviceComponent.buildAssessmentMatrixService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // Handle compute endpoint (POST)
      Matcher computeMatcher = COMPUTE_PATTERN.matcher(path);
      if (computeMatcher.matches()) {
        if (!method.equals("POST")) {
          return ResponseBuilder.buildResponse(405, "Method Not Allowed - POST required for compute endpoint");
        }
        String assessmentMatrixId = computeMatcher.group(1);
        return handleComputeAnalytics(assessmentMatrixId, input.getQueryStringParameters(), context);
      }

      // Only support GET requests for read analytics endpoints
      if (!method.equals("GET")) {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

      // GET /dashboard-analytics/overview/{assessmentMatrixId}
      Matcher overviewMatcher = OVERVIEW_PATTERN.matcher(path);
      if (overviewMatcher.matches()) {
        String assessmentMatrixId = overviewMatcher.group(1);
        return handleGetOverview(assessmentMatrixId, input.getQueryStringParameters(), context);
      }

      // GET /dashboard-analytics/team/{assessmentMatrixId}/{teamId}
      Matcher teamMatcher = TEAM_PATTERN.matcher(path);
      if (teamMatcher.matches()) {
        String assessmentMatrixId = teamMatcher.group(1);
        String teamId = teamMatcher.group(2);
        return handleGetTeamAnalytics(assessmentMatrixId, teamId, input.getQueryStringParameters(), context);
      }

      // GET /performance-cycle-summary/{companyId}
      Matcher performanceCycleMatcher = PERFORMANCE_CYCLE_SUMMARY_PATTERN.matcher(path);
      if (performanceCycleMatcher.matches()) {
        String companyId = performanceCycleMatcher.group(1);
        return handleGetPerformanceCycleSummary(companyId, input.getQueryStringParameters());
      }

      return ResponseBuilder.buildResponse(404, "Not Found");

    }
    catch (Exception e) {
      context.getLogger().log("Error in DashboardAnalyticsRequestHandler: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing dashboard analytics request: " + e.getMessage());
    }
  }

  /**
   * Handle POST /dashboard-analytics/compute/{assessmentMatrixId}
   */
  private APIGatewayProxyResponseEvent handleComputeAnalytics(String assessmentMatrixId, Map<String, String> queryParams, Context context) {
    try {
      // Validate tenant access
      String tenantId = extractTenantId(queryParams);
      if (tenantId == null) {
        return ResponseBuilder.buildResponse(400, "Missing required parameter: tenantId");
      }

      // First, verify tenant access by checking the assessment matrix
      APIGatewayProxyResponseEvent accessCheck = verifyTenantAccess(assessmentMatrixId, tenantId, context);
      if (accessCheck != null) {
        return accessCheck; // Return 403 or 404 if access denied or matrix not found
      }

      // Trigger analytics computation
      dashboardAnalyticsService.updateAssessmentMatrixAnalytics(assessmentMatrixId);

      // Return success response
      Map<String, Object> response = Map.of(
          "success", true, "message", "Dashboard analytics computed successfully", "assessmentMatrixId", assessmentMatrixId, "computedAt", java.time.LocalDateTime.now().toString()
      );

      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));

    }
    catch (Exception e) {
      // Log the full exception details
      context.getLogger().log("DashboardAnalyticsRequestHandler: Exception in handleComputeAnalytics for assessmentMatrixId=" + assessmentMatrixId);
      context.getLogger().log("Exception type: " + e.getClass().getSimpleName());
      context.getLogger().log("Exception message: " + e.getMessage());
      context.getLogger().log("Exception stack trace: " + java.util.Arrays.toString(e.getStackTrace()));

      return ResponseBuilder.buildResponse(500, "Failed to compute analytics: " + e.getMessage());
    }
  }

  /**
   * Handle GET /dashboard-analytics/overview/{assessmentMatrixId}
   */
  private APIGatewayProxyResponseEvent handleGetOverview(String assessmentMatrixId, Map<String, String> queryParams, Context context) {
    try {
      // Validate tenant access
      String tenantId = extractTenantId(queryParams);
      if (tenantId == null) {
        return ResponseBuilder.buildResponse(400, "Missing required parameter: tenantId");
      }

      // First, verify tenant access by checking the assessment matrix
      APIGatewayProxyResponseEvent accessCheck = verifyTenantAccess(assessmentMatrixId, tenantId, context);
      if (accessCheck != null) {
        return accessCheck; // Return 403 or 404 if access denied or matrix not found
      }

      Optional<DashboardAnalytics> overviewOpt = dashboardAnalyticsService.getOverview(assessmentMatrixId);

      if (overviewOpt.isEmpty()) {
        // Return empty analytics response (tenant access already verified)
        DashboardAnalyticsOverviewResponse emptyResponse = buildEmptyOverviewResponse(assessmentMatrixId, tenantId);
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(emptyResponse));
      }

      DashboardAnalytics overview = overviewOpt.get();

      // Get all analytics for teams
      List<DashboardAnalytics> allAnalytics = dashboardAnalyticsService.getAllAnalytics(assessmentMatrixId);

      DashboardAnalyticsOverviewResponse response = buildOverviewResponse(overview, allAnalytics);

      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));

    }
    catch (Exception e) {
      // Log the full exception details
      context.getLogger().log("DashboardAnalyticsRequestHandler: Exception in handleGetOverview for assessmentMatrixId=" + assessmentMatrixId);
      context.getLogger().log("Exception type: " + e.getClass().getSimpleName());
      context.getLogger().log("Exception message: " + e.getMessage());
      context.getLogger().log("Exception stack trace: " + java.util.Arrays.toString(e.getStackTrace()));

      return ResponseBuilder.buildResponse(500, "Failed to retrieve overview analytics: " + e.getMessage());
    }
  }

  /**
   * Handle GET /dashboard-analytics/team/{assessmentMatrixId}/{teamId}
   */
  private APIGatewayProxyResponseEvent handleGetTeamAnalytics(String assessmentMatrixId, String teamId, Map<String, String> queryParams, Context context) {
    try {
      // Validate tenant access
      String tenantId = extractTenantId(queryParams);
      if (tenantId == null) {
        return ResponseBuilder.buildResponse(400, "Missing required parameter: tenantId");
      }

      // First, verify tenant access by checking the assessment matrix
      APIGatewayProxyResponseEvent accessCheck = verifyTenantAccess(assessmentMatrixId, tenantId, context);
      if (accessCheck != null) {
        return accessCheck; // Return 403 or 404 if access denied or matrix not found
      }

      Optional<DashboardAnalytics> teamAnalyticsOpt = dashboardAnalyticsService.getTeamAnalytics(assessmentMatrixId, teamId);

      if (teamAnalyticsOpt.isEmpty()) {
        // Return empty team analytics response (tenant access already verified)
        DashboardAnalyticsTeamResponse emptyResponse = buildEmptyTeamResponse(assessmentMatrixId, teamId, tenantId);
        return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(emptyResponse));
      }

      DashboardAnalytics teamAnalytics = teamAnalyticsOpt.get();

      DashboardAnalyticsTeamResponse response = buildTeamResponse(teamAnalytics);

      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));

    }
    catch (Exception e) {
      // Log the full exception details
      context.getLogger().log("DashboardAnalyticsRequestHandler: Exception in handleGetTeamAnalytics for assessmentMatrixId=" + assessmentMatrixId + ", teamId=" + teamId);
      context.getLogger().log("Exception type: " + e.getClass().getSimpleName());
      context.getLogger().log("Exception message: " + e.getMessage());
      context.getLogger().log("Exception stack trace: " + java.util.Arrays.toString(e.getStackTrace()));

      return ResponseBuilder.buildResponse(500, "Failed to retrieve team analytics: " + e.getMessage());
    }
  }

  /**
   * Handle GET /performance-cycle-summary/{companyId}
   */
  private APIGatewayProxyResponseEvent handleGetPerformanceCycleSummary(String companyId, Map<String, String> queryParams) {
    try {
      // Validate tenant access
      String tenantId = extractTenantId(queryParams);
      if (tenantId == null) {
        return ResponseBuilder.buildResponse(400, "Missing required parameter: tenantId");
      }

      // Verify tenant access
      if (!tenantId.equals(companyId)) {
        return ResponseBuilder.buildResponse(403, "Access denied to this company data");
      }

      // TODO: Implement performance cycle summary logic
      // For now, return placeholder response
      PerformanceCycleSummaryResponse response = PerformanceCycleSummaryResponse.builder().companyId(companyId).companyName("Sample Company").performanceCycles(new ArrayList<>()).build();

      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));

    }
    catch (Exception e) {
      return ResponseBuilder.buildResponse(500, "Failed to retrieve performance cycle summary: " + e.getMessage());
    }
  }

  /**
   * Build overview response from dashboard analytics data
   */
  private DashboardAnalyticsOverviewResponse buildOverviewResponse(DashboardAnalytics overview, List<DashboardAnalytics> allAnalytics) {
    try {
      // Parse analytics data from JSON
      Map<String, Object> analyticsData = parseAnalyticsData(overview.getAnalyticsDataJson());

      // Build metadata using denormalized names
      DashboardAnalyticsOverviewResponse.Metadata metadata = DashboardAnalyticsOverviewResponse.Metadata.builder().assessmentMatrixId(overview.getAssessmentMatrixId()).companyName(overview.getCompanyName() != null ? overview.getCompanyName() : "N/A").performanceCycle(overview.getPerformanceCycleName() != null ? overview.getPerformanceCycleName() : "N/A").assessmentMatrixName(overview.getAssessmentMatrixName() != null ? overview.getAssessmentMatrixName() : "N/A").lastUpdated(overview.getLastUpdated().toString()).build();

      // Extract top/bottom pillars and categories from analytics data
      TopBottomAnalytics topBottomAnalytics = extractTopBottomAnalytics(analyticsData);

      // Build summary
      DashboardAnalyticsOverviewResponse.Summary summary = DashboardAnalyticsOverviewResponse.Summary.builder().generalAverage(overview.getGeneralAverage()).topPillar(topBottomAnalytics.getTopPillar()).bottomPillar(topBottomAnalytics.getBottomPillar()).topCategory(topBottomAnalytics.getTopCategory()).bottomCategory(topBottomAnalytics.getBottomCategory()).totalEmployees(overview.getEmployeeCount()).completionPercentage(overview.getCompletionPercentage()).build();

      // Build team overviews (only include TEAM scope records)
      List<DashboardAnalyticsOverviewResponse.TeamOverview> teams = allAnalytics.stream().filter(analytics -> AnalyticsScope.TEAM.equals(analytics.getScope())).map(this::buildTeamOverview).collect(Collectors.toList());

      return DashboardAnalyticsOverviewResponse.builder().metadata(metadata).summary(summary).teams(teams).build();

    }
    catch (Exception e) {
      throw new RuntimeException("Failed to build overview response", e);
    }
  }

  /**
   * Build team response from dashboard analytics data
   */
  private DashboardAnalyticsTeamResponse buildTeamResponse(DashboardAnalytics teamAnalytics) {
    try {
      // Parse analytics data from JSON
      Map<String, Object> analyticsData = parseAnalyticsData(teamAnalytics.getAnalyticsDataJson());

      // Build pillar scores (same structure as overview endpoint)
      Map<String, DashboardAnalyticsTeamResponse.PillarScore> pillarScores = buildTeamPillarScoresMap(analyticsData);

      // Extract word cloud from analytics data
      DashboardAnalyticsTeamResponse.WordCloud wordCloud = buildTeamWordCloud(analyticsData);

      return DashboardAnalyticsTeamResponse.builder().teamId(teamAnalytics.getTeamId()).teamName(teamAnalytics.getTeamName()).totalScore(teamAnalytics.getGeneralAverage()).employeeCount(teamAnalytics.getEmployeeCount()).completionPercentage(teamAnalytics.getCompletionPercentage()).pillarScores(pillarScores).wordCloud(wordCloud).build();

    }
    catch (Exception e) {
      throw new RuntimeException("Failed to build team response", e);
    }
  }

  /**
   * Build team overview from dashboard analytics
   */
  private DashboardAnalyticsOverviewResponse.TeamOverview buildTeamOverview(DashboardAnalytics analytics) {
    try {
      // Parse analytics data to extract pillar scores
      Map<String, Object> analyticsData = parseAnalyticsData(analytics.getAnalyticsDataJson());
      Map<String, DashboardAnalyticsOverviewResponse.PillarScore> pillarScores = buildPillarScoresMap(analyticsData);

      return DashboardAnalyticsOverviewResponse.TeamOverview.builder().teamId(analytics.getTeamId()).teamName(analytics.getTeamName()).totalScore(analytics.getGeneralAverage()).employeeCount(analytics.getEmployeeCount()).completionPercentage(analytics.getCompletionPercentage()).pillarScores(pillarScores).build();

    }
    catch (Exception e) {
      throw new RuntimeException("Failed to build team overview", e);
    }
  }


  /**
   * Parse analytics data JSON
   */
  private Map<String, Object> parseAnalyticsData(String analyticsDataJson) {
    try {
      if (analyticsDataJson == null || analyticsDataJson.trim().isEmpty()) {
        return Map.of();
      }
      return objectMapper.readValue(analyticsDataJson, new TypeReference<Map<String, Object>>() {
      });
    }
    catch (Exception e) {
      return Map.of();
    }
  }

  /**
   * Build pillar scores map from analytics data
   */
  private Map<String, DashboardAnalyticsOverviewResponse.PillarScore> buildPillarScoresMap(Map<String, Object> analyticsData) {
    Map<String, DashboardAnalyticsOverviewResponse.PillarScore> pillarScores = new HashMap<>();

    try {
      Map<String, Object> pillars = (Map<String, Object>) analyticsData.get("pillars");

      if (pillars == null || pillars.isEmpty()) {
        return pillarScores;
      }

      for (Map.Entry<String, Object> pillarEntry : pillars.entrySet()) {
        Map<String, Object> pillarData = (Map<String, Object>) pillarEntry.getValue();

        String pillarName = (String) pillarData.get("name");
        if (pillarName == null) {
          continue; // Skip pillars without names
        }

        DashboardAnalyticsOverviewResponse.PillarScore pillarScore = buildPillarScore(pillarData);
        pillarScores.put(pillarName, pillarScore);
      }

    }
    catch (Exception e) {
      // Log error but don't fail the entire response - return empty map
      return new HashMap<>();
    }

    return pillarScores;
  }

  /**
   * Build pillar score from pillar data including categories
   */
  private DashboardAnalyticsOverviewResponse.PillarScore buildPillarScore(Map<String, Object> pillarData) {
    String name = (String) pillarData.get("name");
    Double percentage = (Double) pillarData.get("percentage");
    Double actualScore = (Double) pillarData.get("actualScore");
    Double potentialScore = (Double) pillarData.get("potentialScore");
    Double gapFromPotential = (Double) pillarData.get("gapFromPotential");

    // Build categories list
    List<DashboardAnalyticsOverviewResponse.CategoryScore> categories = buildCategoryScores(pillarData);

    return DashboardAnalyticsOverviewResponse.PillarScore.builder().name(name != null ? name : "Unknown Pillar").score(percentage != null ? percentage : 0.0).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).gapFromPotential(gapFromPotential != null ? gapFromPotential : 0.0).categories(categories).build();
  }

  /**
   * Build category scores from pillar data
   */
  private List<DashboardAnalyticsOverviewResponse.CategoryScore> buildCategoryScores(Map<String, Object> pillarData) {
    List<DashboardAnalyticsOverviewResponse.CategoryScore> categoryScores = new ArrayList<>();

    try {
      Map<String, Object> categories = (Map<String, Object>) pillarData.get("categories");

      if (categories == null || categories.isEmpty()) {
        return categoryScores;
      }

      for (Map.Entry<String, Object> categoryEntry : categories.entrySet()) {
        Map<String, Object> categoryData = (Map<String, Object>) categoryEntry.getValue();

        String categoryName = (String) categoryData.get("name");
        if (categoryName == null) {
          continue; // Skip categories without names
        }

        Double percentage = (Double) categoryData.get("percentage");
        Double actualScore = (Double) categoryData.get("actualScore");
        Double potentialScore = (Double) categoryData.get("potentialScore");

        // Calculate gap from potential if not provided
        Double gapFromPotential = percentage != null ? 100.0 - percentage : 0.0;

        DashboardAnalyticsOverviewResponse.CategoryScore categoryScore = DashboardAnalyticsOverviewResponse.CategoryScore.builder().name(categoryName).score(percentage != null ? percentage : 0.0).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).gapFromPotential(gapFromPotential).build();

        categoryScores.add(categoryScore);
      }

    }
    catch (Exception e) {
      // Log error but don't fail - return empty list
      return new ArrayList<>();
    }

    return categoryScores;
  }

  /**
   * Extract tenant ID from query parameters
   */
  private String extractTenantId(Map<String, String> queryParams) {
    return queryParams != null ? queryParams.get("tenantId") : null;
  }

  /**
   * Verify tenant access to assessment matrix
   * Returns null if access is allowed, or error response if access denied
   */
  private APIGatewayProxyResponseEvent verifyTenantAccess(String assessmentMatrixId, String tenantId, Context context) {
    try {
      // Get the assessment matrix to verify tenant access
      Optional<AssessmentMatrix> matrixOpt = assessmentMatrixService.findById(assessmentMatrixId);

      if (matrixOpt.isEmpty()) {
        context.getLogger().log("DashboardAnalyticsRequestHandler: Assessment matrix not found: " + assessmentMatrixId);
        return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
      }

      AssessmentMatrix matrix = matrixOpt.get();

      // Verify that the tenant ID matches the company ID (tenant) of the assessment matrix
      if (!tenantId.equals(matrix.getTenantId())) {
        return ResponseBuilder.buildResponse(403, "Access denied to this assessment matrix");
      }

      return null; // Access allowed

    }
    catch (Exception e) {
      context.getLogger().log("DashboardAnalyticsRequestHandler: Error verifying tenant access: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error verifying access permissions");
    }
  }


  /**
   * Build empty overview response when no analytics are available
   */
  private DashboardAnalyticsOverviewResponse buildEmptyOverviewResponse(String assessmentMatrixId, String tenantId) {
    try {
      // Build empty metadata
      DashboardAnalyticsOverviewResponse.Metadata metadata = DashboardAnalyticsOverviewResponse.Metadata.builder().assessmentMatrixId(assessmentMatrixId).companyName("N/A") // Will be populated when analytics are generated
          .performanceCycle("N/A") // Will be populated when analytics are generated
          .assessmentMatrixName("N/A") // Will be populated when analytics are generated
          .lastUpdated(null).build();

      // Build empty summary
      DashboardAnalyticsOverviewResponse.Summary summary = DashboardAnalyticsOverviewResponse.Summary.builder().generalAverage(0.0).totalEmployees(0).completionPercentage(0.0).build();

      // Empty teams list
      List<DashboardAnalyticsOverviewResponse.TeamOverview> teams = new ArrayList<>();

      return DashboardAnalyticsOverviewResponse.builder().metadata(metadata).summary(summary).teams(teams).build();

    }
    catch (Exception e) {
      throw new RuntimeException("Failed to build empty overview response", e);
    }
  }

  /**
   * Build empty team response when no analytics are available
   */
  private DashboardAnalyticsTeamResponse buildEmptyTeamResponse(String assessmentMatrixId, String teamId, String tenantId) {
    try {
      return DashboardAnalyticsTeamResponse.builder().teamId(teamId).teamName("Unknown Team") // Will be populated when analytics are generated
          .totalScore(0.0).employeeCount(0).completionPercentage(0.0).pillarScores(new HashMap<>()) // Empty pillar scores map
          .wordCloud(buildEmptyWordCloud()) // Empty word cloud
          .build();

    }
    catch (Exception e) {
      throw new RuntimeException("Failed to build empty team response", e);
    }
  }

  /**
   * Extract top/bottom pillars and categories from analytics data
   */
  private TopBottomAnalytics extractTopBottomAnalytics(Map<String, Object> analyticsData) {
    try {
      Map<String, Object> pillars = (Map<String, Object>) analyticsData.get("pillars");

      if (pillars == null || pillars.isEmpty()) {
        return new TopBottomAnalytics(null, null, null, null);
      }

      DashboardAnalyticsOverviewResponse.PillarSummary topPillar = null;
      DashboardAnalyticsOverviewResponse.PillarSummary bottomPillar = null;
      DashboardAnalyticsOverviewResponse.CategorySummary topCategory = null;
      DashboardAnalyticsOverviewResponse.CategorySummary bottomCategory = null;

      double maxPillarPercentage = Double.MIN_VALUE;
      double minPillarPercentage = Double.MAX_VALUE;
      double maxCategoryPercentage = Double.MIN_VALUE;
      double minCategoryPercentage = Double.MAX_VALUE;

      // Process each pillar
      for (Map.Entry<String, Object> pillarEntry : pillars.entrySet()) {
        Map<String, Object> pillarData = (Map<String, Object>) pillarEntry.getValue();

        String pillarName = (String) pillarData.get("name");
        Double pillarPercentage = (Double) pillarData.get("percentage");
        Double actualScore = (Double) pillarData.get("actualScore");
        Double potentialScore = (Double) pillarData.get("potentialScore");

        if (pillarPercentage != null) {
          // Check for top pillar
          if (pillarPercentage > maxPillarPercentage) {
            maxPillarPercentage = pillarPercentage;
            topPillar = DashboardAnalyticsOverviewResponse.PillarSummary.builder().name(pillarName).percentage(pillarPercentage).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).build();
          }

          // Check for bottom pillar
          if (pillarPercentage < minPillarPercentage) {
            minPillarPercentage = pillarPercentage;
            bottomPillar = DashboardAnalyticsOverviewResponse.PillarSummary.builder().name(pillarName).percentage(pillarPercentage).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).build();
          }
        }

        // Process categories within this pillar
        Map<String, Object> categories = (Map<String, Object>) pillarData.get("categories");
        if (categories != null) {
          for (Map.Entry<String, Object> categoryEntry : categories.entrySet()) {
            Map<String, Object> categoryData = (Map<String, Object>) categoryEntry.getValue();

            String categoryName = (String) categoryData.get("name");
            Double categoryPercentage = (Double) categoryData.get("percentage");
            Double categoryActualScore = (Double) categoryData.get("actualScore");
            Double categoryPotentialScore = (Double) categoryData.get("potentialScore");

            if (categoryPercentage != null) {
              // Check for top category
              if (categoryPercentage > maxCategoryPercentage) {
                maxCategoryPercentage = categoryPercentage;
                topCategory = DashboardAnalyticsOverviewResponse.CategorySummary.builder().name(categoryName).pillar(pillarName).percentage(categoryPercentage).actualScore(categoryActualScore != null ? categoryActualScore : 0.0).potentialScore(categoryPotentialScore != null ? categoryPotentialScore : 0.0).build();
              }

              // Check for bottom category
              if (categoryPercentage < minCategoryPercentage) {
                minCategoryPercentage = categoryPercentage;
                bottomCategory = DashboardAnalyticsOverviewResponse.CategorySummary.builder().name(categoryName).pillar(pillarName).percentage(categoryPercentage).actualScore(categoryActualScore != null ? categoryActualScore : 0.0).potentialScore(categoryPotentialScore != null ? categoryPotentialScore : 0.0).build();
              }
            }
          }
        }
      }

      return new TopBottomAnalytics(topPillar, bottomPillar, topCategory, bottomCategory);

    }
    catch (Exception e) {
      // Log error but don't fail the entire response
      return new TopBottomAnalytics(null, null, null, null);
    }
  }

  /**
   * Helper class to hold top/bottom analytics results
   */
  private static class TopBottomAnalytics {
    private final DashboardAnalyticsOverviewResponse.PillarSummary topPillar;
    private final DashboardAnalyticsOverviewResponse.PillarSummary bottomPillar;
    private final DashboardAnalyticsOverviewResponse.CategorySummary topCategory;
    private final DashboardAnalyticsOverviewResponse.CategorySummary bottomCategory;

    public TopBottomAnalytics(DashboardAnalyticsOverviewResponse.PillarSummary topPillar, DashboardAnalyticsOverviewResponse.PillarSummary bottomPillar, DashboardAnalyticsOverviewResponse.CategorySummary topCategory, DashboardAnalyticsOverviewResponse.CategorySummary bottomCategory) {
      this.topPillar = topPillar;
      this.bottomPillar = bottomPillar;
      this.topCategory = topCategory;
      this.bottomCategory = bottomCategory;
    }

    public DashboardAnalyticsOverviewResponse.PillarSummary getTopPillar() {
      return topPillar;
    }

    public DashboardAnalyticsOverviewResponse.PillarSummary getBottomPillar() {
      return bottomPillar;
    }

    public DashboardAnalyticsOverviewResponse.CategorySummary getTopCategory() {
      return topCategory;
    }

    public DashboardAnalyticsOverviewResponse.CategorySummary getBottomCategory() {
      return bottomCategory;
    }
  }

  /**
   * Build pillar scores map for team response (reuse overview logic)
   */
  private Map<String, DashboardAnalyticsTeamResponse.PillarScore> buildTeamPillarScoresMap(Map<String, Object> analyticsData) {
    Map<String, DashboardAnalyticsTeamResponse.PillarScore> pillarScores = new HashMap<>();

    try {
      Map<String, Object> pillars = (Map<String, Object>) analyticsData.get("pillars");

      if (pillars == null || pillars.isEmpty()) {
        return pillarScores;
      }

      for (Map.Entry<String, Object> pillarEntry : pillars.entrySet()) {
        Map<String, Object> pillarData = (Map<String, Object>) pillarEntry.getValue();

        String pillarName = (String) pillarData.get("name");
        if (pillarName == null) {
          continue; // Skip pillars without names
        }

        DashboardAnalyticsTeamResponse.PillarScore pillarScore = buildTeamPillarScore(pillarData);
        pillarScores.put(pillarName, pillarScore);
      }

    }
    catch (Exception e) {
      // Log error but don't fail the entire response - return empty map
      return new HashMap<>();
    }

    return pillarScores;
  }

  /**
   * Build individual team pillar score from pillar data
   */
  private DashboardAnalyticsTeamResponse.PillarScore buildTeamPillarScore(Map<String, Object> pillarData) {
    String name = (String) pillarData.get("name");
    Double percentage = (Double) pillarData.get("percentage");
    Double actualScore = (Double) pillarData.get("actualScore");
    Double potentialScore = (Double) pillarData.get("potentialScore");
    Double gapFromPotential = (Double) pillarData.get("gapFromPotential");

    // Build categories list
    List<DashboardAnalyticsTeamResponse.CategoryScore> categories = buildTeamCategoryScores(pillarData);

    return DashboardAnalyticsTeamResponse.PillarScore.builder().name(name != null ? name : "Unknown Pillar").score(percentage != null ? percentage : 0.0).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).gapFromPotential(gapFromPotential != null ? gapFromPotential : 0.0).categories(categories).build();
  }

  /**
   * Build category scores for team response
   */
  private List<DashboardAnalyticsTeamResponse.CategoryScore> buildTeamCategoryScores(Map<String, Object> pillarData) {
    List<DashboardAnalyticsTeamResponse.CategoryScore> categoryScores = new ArrayList<>();

    try {
      Map<String, Object> categories = (Map<String, Object>) pillarData.get("categories");

      if (categories == null || categories.isEmpty()) {
        return categoryScores;
      }

      for (Map.Entry<String, Object> categoryEntry : categories.entrySet()) {
        Map<String, Object> categoryData = (Map<String, Object>) categoryEntry.getValue();

        String categoryName = (String) categoryData.get("name");
        if (categoryName == null) {
          continue; // Skip categories without names
        }

        Double percentage = (Double) categoryData.get("percentage");
        Double actualScore = (Double) categoryData.get("actualScore");
        Double potentialScore = (Double) categoryData.get("potentialScore");

        // Calculate gap from potential if not provided
        Double gapFromPotential = percentage != null ? 100.0 - percentage : 0.0;

        DashboardAnalyticsTeamResponse.CategoryScore categoryScore = DashboardAnalyticsTeamResponse.CategoryScore.builder().name(categoryName).score(percentage != null ? percentage : 0.0).actualScore(actualScore != null ? actualScore : 0.0).potentialScore(potentialScore != null ? potentialScore : 0.0).gapFromPotential(gapFromPotential).build();

        categoryScores.add(categoryScore);
      }

    }
    catch (Exception e) {
      // Log error but don't fail - return empty list
      return new ArrayList<>();
    }

    return categoryScores;
  }

  /**
   * Build word cloud for team response
   */
  private DashboardAnalyticsTeamResponse.WordCloud buildTeamWordCloud(Map<String, Object> analyticsData) {
    try {
      Map<String, Object> wordCloudData = (Map<String, Object>) analyticsData.get("wordCloud");

      if (wordCloudData == null || wordCloudData.isEmpty()) {
        return buildEmptyWordCloud();
      }

      String status = (String) wordCloudData.get("status");
      Integer totalResponses = (Integer) wordCloudData.get("totalResponses");
      List<Map<String, Object>> wordsData = (List<Map<String, Object>>) wordCloudData.get("words");

      List<DashboardAnalyticsTeamResponse.WordFrequency> words = new ArrayList<>();

      if (wordsData != null) {
        for (Map<String, Object> wordData : wordsData) {
          String text = (String) wordData.get("text");
          Integer count = (Integer) wordData.get("count");

          if (text != null && count != null) {
            words.add(DashboardAnalyticsTeamResponse.WordFrequency.builder().text(text).count(count).build());
          }
        }
      }

      return DashboardAnalyticsTeamResponse.WordCloud.builder().words(words).totalResponses(totalResponses != null ? totalResponses : 0).status(status != null ? status : "none").build();

    }
    catch (Exception e) {
      // Log error but don't fail - return empty word cloud
      return buildEmptyWordCloud();
    }
  }

  /**
   * Build empty word cloud
   */
  private DashboardAnalyticsTeamResponse.WordCloud buildEmptyWordCloud() {
    return DashboardAnalyticsTeamResponse.WordCloud.builder().words(new ArrayList<>()).totalResponses(0).status("none").build();
  }
}