package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.EmployeeAssessment;
import com.agilecheckup.persistency.entity.person.Gender;
import com.agilecheckup.persistency.entity.person.GenderPronoun;
import com.agilecheckup.persistency.entity.person.PersonDocumentType;
import com.agilecheckup.service.EmployeeAssessmentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class EmployeeAssessmentRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/employeeassessments/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/employeeassessments/([^/]+)/?$");
  private static final Pattern UPDATE_SCORE_PATTERN = Pattern.compile("^/employeeassessments/([^/]+)/score/?$");

  private final EmployeeAssessmentService employeeAssessmentService;
  private final ObjectMapper objectMapper;

  public EmployeeAssessmentRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.employeeAssessmentService = serviceComponent.buildEmployeeAssessmentService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /employeeassessments
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
      }
      // GET /employeeassessments/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /employeeassessments
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // POST /employeeassessments/{id}/score - Special endpoint to update assessment score
      else if (method.equals("POST") && UPDATE_SCORE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path.replace("/score", ""));
        return handleUpdateScore(id, input.getBody());
      }
      // DELETE /employeeassessments/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in employee assessment endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing employee assessment request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(employeeAssessmentService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<EmployeeAssessment> employeeAssessment = employeeAssessmentService.findById(id);

    if (employeeAssessment.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(employeeAssessment.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Employee assessment not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Parse enums from strings
    Gender gender = Gender.valueOf((String) requestMap.get("gender"));
    GenderPronoun genderPronoun = GenderPronoun.valueOf((String) requestMap.get("genderPronoun"));
    PersonDocumentType documentType = null;
    if (requestMap.get("documentType") != null) {
      documentType = PersonDocumentType.valueOf((String) requestMap.get("documentType"));
    }

    Optional<EmployeeAssessment> employeeAssessment = employeeAssessmentService.create(
        (String) requestMap.get("assessmentMatrixId"),
        (String) requestMap.get("teamId"),
        (String) requestMap.get("name"),
        (String) requestMap.get("email"),
        (String) requestMap.get("documentNumber"),
        documentType,
        gender,
        genderPronoun
    );

    if (employeeAssessment.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(employeeAssessment.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create employee assessment");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdateScore(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);
    String tenantId = (String) requestMap.get("tenantId");

    EmployeeAssessment employeeAssessment = employeeAssessmentService.updateEmployeeAssessmentScore(id, tenantId);

    if (employeeAssessment != null) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(employeeAssessment));
    } else {
      return ResponseBuilder.buildResponse(404, "Employee assessment not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<EmployeeAssessment> employeeAssessment = employeeAssessmentService.findById(id);

    if (employeeAssessment.isPresent()) {
      employeeAssessmentService.delete(employeeAssessment.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Employee assessment not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /employeeassessments/{id}
    String pathWithoutParams = path;
    if (path.contains("/score")) {
      pathWithoutParams = path.replace("/score", "");
    }
    return pathWithoutParams.substring(pathWithoutParams.lastIndexOf("/") + 1);
  }
}