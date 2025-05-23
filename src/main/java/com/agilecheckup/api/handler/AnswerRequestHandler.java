package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.question.Answer;
import com.agilecheckup.service.AnswerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AnswerRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/answers/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/answers/([^/]+)/?$");
  private static final Pattern GET_BY_EMPLOYEE_ASSESSMENT_PATTERN = Pattern.compile("^/answers/employeeassessment/([^/]+)/?$");

  private final AnswerService answerService;
  private final ObjectMapper objectMapper;

  public AnswerRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.answerService = serviceComponent.buildAnswerService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /answers
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
      }
      // GET /answers/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // GET /answers/employeeassessment/{employeeAssessmentId}
      else if (method.equals("GET") && GET_BY_EMPLOYEE_ASSESSMENT_PATTERN.matcher(path).matches()) {
        String employeeAssessmentId = path.substring(path.lastIndexOf("/") + 1);
        return handleGetByEmployeeAssessmentId(employeeAssessmentId, input.getQueryStringParameters());
      }
      // POST /answers
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /answers/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // DELETE /answers/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in answer endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing answer request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(answerService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Answer> answer = answerService.findById(id);

    if (answer.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(answer.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Answer not found");
    }
  }

  private APIGatewayProxyResponseEvent handleGetByEmployeeAssessmentId(String employeeAssessmentId, Map<String, String> queryParams) throws Exception {
    String tenantId = queryParams != null ? queryParams.get("tenantId") : null;
    if (tenantId == null) {
      return ResponseBuilder.buildResponse(400, "Missing required query parameter: tenantId");
    }

    // We need to use the repository directly since it's not exposed through the service
    // This will need to be added to the AnswerService
    return ResponseBuilder.buildResponse(501, "This endpoint is not yet implemented");
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Parse the LocalDateTime
    LocalDateTime answeredAt = LocalDateTime.parse((String) requestMap.get("answeredAt"));

    Optional<Answer> answer = answerService.create(
        (String) requestMap.get("employeeAssessmentId"),
        (String) requestMap.get("questionId"),
        answeredAt,
        (String) requestMap.get("value"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("notes")
    );

    if (answer.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(answer.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create answer");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Parse the LocalDateTime
    LocalDateTime answeredAt = LocalDateTime.parse((String) requestMap.get("answeredAt"));

    Optional<Answer> answer = answerService.update(
        id,
        answeredAt,
        (String) requestMap.get("value"),
        (String) requestMap.get("notes")
    );

    if (answer.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(answer.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Answer not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Answer> answer = answerService.findById(id);

    if (answer.isPresent()) {
      answerService.delete(answer.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Answer not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /answers/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}