package com.agilecheckup.api.handler;

import com.agilecheckup.service.DepartmentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Department;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class DepartmentRequestHandler implements RequestHandlerStrategy {

  private final DepartmentService departmentService;
  private final ObjectMapper objectMapper;

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/departments/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/departments/([^/]+)/?$");

  public DepartmentRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.departmentService = serviceComponent.buildDepartmentService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /departments
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll(input);
      }
      // GET /departments/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /departments
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /departments/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // DELETE /departments/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in department endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing department request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception {
    Map<String, String> queryParams = input.getQueryStringParameters();
    
    if (queryParams != null && queryParams.containsKey("tenantId")) {
      String tenantId = queryParams.get("tenantId");
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(departmentService.findAllByTenantId(tenantId)));
    } else {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(departmentService.findAll()));
    }
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Department> department = departmentService.findById(id);

    if (department.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(department.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Department not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Department> department = departmentService.create(
        (String) requestMap.get("name"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("companyId")
    );

    if (department.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(department.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create department");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Department> department = departmentService.update(
        id,
        (String) requestMap.get("name"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("companyId")
    );

    if (department.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(department.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Department not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Department> department = departmentService.findById(id);

    if (department.isPresent()) {
      departmentService.deleteById(id);
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Department not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /departments/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}