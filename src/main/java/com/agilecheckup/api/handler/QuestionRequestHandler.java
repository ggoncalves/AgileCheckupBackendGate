package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.QuestionType;
import com.agilecheckup.persistency.entity.question.Question;
import com.agilecheckup.persistency.entity.question.QuestionOption;
import com.agilecheckup.service.QuestionService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class QuestionRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/questions/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/questions/([^/]+)/?$");
  private static final Pattern GET_BY_ASSESSMENT_MATRIX_PATTERN = Pattern.compile("^/questions/matrix/([^/]+)/?$");
  private static final Pattern CUSTOM_QUESTION_PATTERN = Pattern.compile("^/questions/custom/?$");
  private static final Pattern UPDATE_CUSTOM_QUESTION_PATTERN = Pattern.compile("^/questions/([^/]+)/custom/?$");

  private final QuestionService questionService;
  private final ObjectMapper objectMapper;

  public QuestionRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.questionService = serviceComponent.buildQuestionService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /questions
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll(input);
      }
      // GET /questions/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // GET /questions/matrix/{matrixId}
      else if (method.equals("GET") && GET_BY_ASSESSMENT_MATRIX_PATTERN.matcher(path).matches()) {
        String matrixId = path.substring(path.lastIndexOf("/") + 1);
        return handleGetByAssessmentMatrixId(matrixId, input.getQueryStringParameters());
      }
      // POST /questions
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // POST /questions/custom
      else if (method.equals("POST") && CUSTOM_QUESTION_PATTERN.matcher(path).matches()) {
        return handleCreateCustomQuestion(input.getBody());
      }
      // PUT /questions/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // PUT /questions/{id}/custom
      else if (method.equals("PUT") && UPDATE_CUSTOM_QUESTION_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path.replace("/custom", ""));
        return handleUpdateCustomQuestion(id, input.getBody());
      }
      // DELETE /questions/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in question endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing question request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();
    String tenantId = queryParams != null ? queryParams.get("tenantId") : null;
    if (tenantId == null) {
      return ResponseBuilder.buildResponse(400, "Missing required query parameter: tenantId");
    }

    PaginatedQueryList<Question> questions = questionService.findAllByTenantId(tenantId);
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(questions));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Question> question = questionService.findById(id);

    if (question.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(question.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Question not found");
    }
  }

  private APIGatewayProxyResponseEvent handleGetByAssessmentMatrixId(String matrixId, Map<String, String> queryParams) throws Exception {
    String tenantId = queryParams != null ? queryParams.get("tenantId") : null;
    if (tenantId == null) {
      return ResponseBuilder.buildResponse(400, "Missing required query parameter: tenantId");
    }

    List<Question> questions = questionService.findByAssessmentMatrixId(matrixId, tenantId);
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(questions));
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Convert string to enum for QuestionType
    QuestionType questionType = QuestionType.valueOf((String) requestMap.get("questionType"));

    Optional<Question> question = questionService.create(
        (String) requestMap.get("question"),
        questionType,
        (String) requestMap.get("tenantId"),
        Double.valueOf(requestMap.get("points").toString()),
        (String) requestMap.get("assessmentMatrixId"),
        (String) requestMap.get("pillarId"),
        (String) requestMap.get("categoryId"),
        (String) requestMap.get("extraDescription")
    );

    if (question.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(question.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create question");
    }
  }

  private APIGatewayProxyResponseEvent handleCreateCustomQuestion(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Convert string to enum for QuestionType
    QuestionType questionType = QuestionType.valueOf((String) requestMap.get("questionType"));

    // Convert options list
    List<QuestionOption> options = objectMapper.convertValue(
        requestMap.get("options"),
        objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionOption.class)
    );

    Optional<Question> question = questionService.createCustomQuestion(
        (String) requestMap.get("question"),
        questionType,
        (String) requestMap.get("tenantId"),
        (Boolean) requestMap.get("isMultipleChoice"),
        (Boolean) requestMap.get("showFlushed"),
        options,
        (String) requestMap.get("assessmentMatrixId"),
        (String) requestMap.get("pillarId"),
        (String) requestMap.get("categoryId"),
        (String) requestMap.get("extraDescription")
    );

    if (question.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(question.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create custom question");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Convert string to enum for QuestionType
    QuestionType questionType = QuestionType.valueOf((String) requestMap.get("questionType"));

    Optional<Question> question = questionService.update(
        id,
        (String) requestMap.get("question"),
        questionType,
        (String) requestMap.get("tenantId"),
        Double.valueOf(requestMap.get("points").toString()),
        (String) requestMap.get("assessmentMatrixId"),
        (String) requestMap.get("pillarId"),
        (String) requestMap.get("categoryId"),
        (String) requestMap.get("extraDescription")
    );

    if (question.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(question.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Question not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdateCustomQuestion(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    // Convert string to enum for QuestionType
    QuestionType questionType = QuestionType.valueOf((String) requestMap.get("questionType"));

    // Convert options list
    List<QuestionOption> options = objectMapper.convertValue(
        requestMap.get("options"),
        objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionOption.class)
    );

    Optional<Question> question = questionService.updateCustomQuestion(
        id,
        (String) requestMap.get("question"),
        questionType,
        (String) requestMap.get("tenantId"),
        (Boolean) requestMap.get("isMultipleChoice"),
        (Boolean) requestMap.get("showFlushed"),
        options,
        (String) requestMap.get("assessmentMatrixId"),
        (String) requestMap.get("pillarId"),
        (String) requestMap.get("categoryId"),
        (String) requestMap.get("extraDescription")
    );

    if (question.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(question.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Question not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Question> question = questionService.findById(id);

    if (question.isPresent()) {
      questionService.delete(question.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Question not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /questions/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}