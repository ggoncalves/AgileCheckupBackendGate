package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.EmployeeAssessment;
import com.agilecheckup.service.EmployeeAssessmentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agilecheckup.service.dto.EmployeeValidationRequest;
import com.agilecheckup.service.dto.EmployeeValidationResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class EmployeeAssessmentRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/employeeassessments/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/employeeassessments/([^/]+)/?$");
  private static final Pattern UPDATE_SCORE_PATTERN = Pattern.compile("^/employeeassessments/([^/]+)/score/?$");
  private static final Pattern VALIDATE_PATTERN = Pattern.compile("^/employeeassessments/validate/?$");

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
        return handleGetAll(input);
      }
      // POST /employeeassessments/validate
      else if (method.equals("POST") && VALIDATE_PATTERN.matcher(path).matches()) {
        return handleValidateEmployee(input);
      }
      // GET /employeeassessments/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id, input);
      }
      // POST /employeeassessments
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /employeeassessments/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
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

  private APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();
    
    if (queryParams == null || !queryParams.containsKey("tenantId")) {
      return ResponseBuilder.buildResponse(400, "tenantId is required");
    }
    
    String tenantId = queryParams.get("tenantId");
    String assessmentMatrixId = queryParams.get("assessmentMatrixId");
    
    if (assessmentMatrixId != null && !assessmentMatrixId.isEmpty()) {
      // Filter by assessment matrix
      return ResponseBuilder.buildResponse(200, 
          objectMapper.writeValueAsString(employeeAssessmentService.findByAssessmentMatrix(assessmentMatrixId, tenantId)));
    } else {
      // Return all for tenant
      return ResponseBuilder.buildResponse(200, 
          objectMapper.writeValueAsString(employeeAssessmentService.findAllByTenantId(tenantId)));
    }
  }

  private APIGatewayProxyResponseEvent handleGetById(String id, APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();
    
    if (queryParams == null || !queryParams.containsKey("tenantId")) {
      return ResponseBuilder.buildResponse(400, "tenantId is required");
    }
    
    String tenantId = queryParams.get("tenantId");
    Optional<EmployeeAssessment> employeeAssessment = employeeAssessmentService.findById(id, tenantId);

    if (employeeAssessment.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(employeeAssessment.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Employee assessment not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    try {
      EmployeeAssessment employeeAssessment = objectMapper.readValue(requestBody, EmployeeAssessment.class);
      
      // Validate required fields
      if (employeeAssessment.getTenantId() == null || employeeAssessment.getTenantId().isEmpty()) {
        return ResponseBuilder.buildResponse(400, "tenantId is required");
      }
      if (employeeAssessment.getAssessmentMatrixId() == null || employeeAssessment.getAssessmentMatrixId().isEmpty()) {
        return ResponseBuilder.buildResponse(400, "assessmentMatrixId is required");
      }
      if (employeeAssessment.getEmployee() == null || 
          employeeAssessment.getEmployee().getEmail() == null || 
          employeeAssessment.getEmployee().getEmail().isEmpty()) {
        return ResponseBuilder.buildResponse(400, "employee email is required");
      }
      
      // Set default status if not provided
      if (employeeAssessment.getAssessmentStatus() == null) {
        employeeAssessment.setAssessmentStatus(com.agilecheckup.persistency.entity.AssessmentStatus.INVITED);
      }
      if (employeeAssessment.getAnsweredQuestionCount() == null) {
        employeeAssessment.setAnsweredQuestionCount(0);
      }
      
      // Force validation for new entities by clearing the auto-generated ID
      employeeAssessment.setId(null);
      
      // Create through service
      EmployeeAssessment created = employeeAssessmentService.save(employeeAssessment);
      
      if (created != null) {
        return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(created));
      } else {
        return ResponseBuilder.buildResponse(400, "Failed to create employee assessment");
      }
      
    } catch (com.agilecheckup.service.exception.EmployeeAssessmentAlreadyExistsException e) {
      return ResponseBuilder.buildResponse(409, "Duplicate employee assessment: " + e.getMessage());
    } catch (Exception e) {
      throw e;
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    EmployeeAssessment employeeAssessment = objectMapper.readValue(requestBody, EmployeeAssessment.class);
    
    // Validate required fields
    if (employeeAssessment.getTenantId() == null || employeeAssessment.getTenantId().isEmpty()) {
      return ResponseBuilder.buildResponse(400, "tenantId is required");
    }
    
    // Set the ID from path
    employeeAssessment.setId(id);
    
    // Update through service
    EmployeeAssessment updated = employeeAssessmentService.save(employeeAssessment);
    
    if (updated != null) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(updated));
    } else {
      return ResponseBuilder.buildResponse(404, "Employee assessment not found or update failed");
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
    try {
      employeeAssessmentService.deleteById(id);
      return ResponseBuilder.buildResponse(204, "");
    } catch (Exception e) {
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
  
  private APIGatewayProxyResponseEvent handleValidateEmployee(APIGatewayProxyRequestEvent input) throws Exception {
    try {
      EmployeeValidationRequest request = parseValidationRequest(input.getBody());
      
      Optional<APIGatewayProxyResponseEvent> validationError = validateRequestFields(request);
      if (validationError.isPresent()) {
        return validationError.get();
      }
      
      return processEmployeeValidation(request);
      
    } catch (Exception e) {
      return buildErrorResponse(e);
    }
  }
  
  private EmployeeValidationRequest parseValidationRequest(String requestBody) throws Exception {
    if (StringUtils.isBlank(requestBody)) {
      throw new IllegalArgumentException("Request body is required");
    }
    return objectMapper.readValue(requestBody, EmployeeValidationRequest.class);
  }
  
  private Optional<APIGatewayProxyResponseEvent> validateRequestFields(EmployeeValidationRequest request) {
    if (request == null) {
      return Optional.of(ResponseBuilder.buildResponse(400, "Invalid request format"));
    }
    
    if (StringUtils.isBlank(request.getEmail())) {
      return Optional.of(ResponseBuilder.buildResponse(400, "email is required"));
    }
    
    if (StringUtils.isBlank(request.getAssessmentMatrixId())) {
      return Optional.of(ResponseBuilder.buildResponse(400, "assessmentMatrixId is required"));
    }
    
    if (StringUtils.isBlank(request.getTenantId())) {
      return Optional.of(ResponseBuilder.buildResponse(400, "tenantId is required"));
    }
    
    return Optional.empty();
  }
  
  private APIGatewayProxyResponseEvent processEmployeeValidation(EmployeeValidationRequest request) throws Exception {
    EmployeeValidationResponse response = employeeAssessmentService.validateEmployee(request);
    
    int httpStatus = determineHttpStatus(response);
    String responseBody = objectMapper.writeValueAsString(response);
    
    return ResponseBuilder.buildResponse(httpStatus, responseBody);
  }
  
  private int determineHttpStatus(EmployeeValidationResponse response) {
    if (response == null || StringUtils.isBlank(response.getStatus())) {
      return 500;
    }
    
    return "ERROR".equals(response.getStatus()) ? 404 : 200;
  }
  
  private APIGatewayProxyResponseEvent buildErrorResponse(Exception e) {
    String errorMessage = StringUtils.isNotBlank(e.getMessage()) 
        ? "Error validating employee: " + e.getMessage()
        : "Error validating employee: Unknown error occurred";
    
    return ResponseBuilder.buildResponse(500, errorMessage);
  }
}