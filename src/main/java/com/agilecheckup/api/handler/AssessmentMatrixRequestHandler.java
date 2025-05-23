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

public class AssessmentMatrixRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/assessmentmatrices/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/assessmentmatrices/([^/]+)/?$");
  private static final Pattern UPDATE_POTENTIAL_SCORE_PATTERN = Pattern.compile("^/assessmentmatrices/([^/]+)/potentialscore/?$");

  private final AssessmentMatrixService assessmentMatrixService;
  private final ObjectMapper objectMapper;

  public AssessmentMatrixRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.assessmentMatrixService = serviceComponent.buildAssessmentMatrixService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /assessmentmatrices
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
      }
      // GET /assessmentmatrices/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /assessmentmatrices
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody(), context);
      }
      // PUT /assessmentmatrices/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody(), context);
      }
      // POST /assessmentmatrices/{id}/potentialscore - Special endpoint to update potential score
      else if (method.equals("POST") && UPDATE_POTENTIAL_SCORE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path.replace("/potentialscore", ""));
        return handleUpdatePotentialScore(id, input.getBody());
      }
      // DELETE /assessmentmatrices/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      e.printStackTrace();
      context.getLogger().log("Error in assessment matrix endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing assessment matrix request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrixService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.findById(id);

    if (assessmentMatrix.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(assessmentMatrix.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody, Context context) throws Exception {
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
      e.printStackTrace();
      context.getLogger().log("Detailed error: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error creating assessment matrix: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody, Context context) throws Exception {
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
      e.printStackTrace();
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

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<AssessmentMatrix> assessmentMatrix = assessmentMatrixService.findById(id);

    if (assessmentMatrix.isPresent()) {
      assessmentMatrixService.delete(assessmentMatrix.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Assessment matrix not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /assessmentmatrices/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}