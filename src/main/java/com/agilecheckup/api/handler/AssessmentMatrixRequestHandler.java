package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.AssessmentMatrix;
import com.agilecheckup.persistency.entity.Category;
import com.agilecheckup.persistency.entity.Pillar;
import com.agilecheckup.service.AssessmentMatrixService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AssessmentMatrixRequestHandler extends AbstractCrudRequestHandler<AssessmentMatrix> {

  private static final Pattern UPDATE_POTENTIAL_SCORE_PATTERN = Pattern.compile("^/assessmentmatrices/([^/]+)/potentialscore/?$");

  private final AssessmentMatrixService assessmentMatrixService;

  public AssessmentMatrixRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    super(objectMapper, "assessmentmatrices");
    this.assessmentMatrixService = serviceComponent.buildAssessmentMatrixService();
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
    return Optional.empty();
  }

  @Override
  protected APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();

    if (queryParams != null && queryParams.containsKey("tenantId")) {
      String tenantId = queryParams.get("tenantId");
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrixService.findAllByTenantId(tenantId)));
    }

    // No tenantId provided - return error for security
    return ResponseBuilder.buildResponse(400, "tenantId is required");
  }

  @Override
  protected APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.findById(id);

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

      // Create the pillar map with proper types
      Map<String, Pillar> pillarMap = buildPillarMap(requestMap);

      // Now create the assessment matrix with properly typed pillar map
      Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.create(
          (String) requestMap.get("name"),
          (String) requestMap.get("description"),
          (String) requestMap.get("tenantId"),
          (String) requestMap.get("performanceCycleId"),
          pillarMap
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

      // Create the pillar map with proper types
      Map<String, Pillar> pillarMap = buildPillarMap(requestMap);

      Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.update(
          id,
          (String) requestMap.get("name"),
          (String) requestMap.get("description"),
          (String) requestMap.get("tenantId"),
          (String) requestMap.get("performanceCycleId"),
          pillarMap
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

  private APIGatewayProxyResponseEvent handleUpdatePotentialScore(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);
    String tenantId = (String) requestMap.get("tenantId");

    AssessmentMatrix assessmentMatrix = assessmentMatrixService.updateCurrentPotentialScore(id, tenantId);

    if (assessmentMatrix != null) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrix));
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found or update failed");
    }
  }

  @Override
  protected APIGatewayProxyResponseEvent handleDelete(String id) throws Exception {
    Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.findById(id);

    if (assessmentMatrix.isPresent()) {
      assessmentMatrixService.delete(assessmentMatrix.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
    }
  }

}