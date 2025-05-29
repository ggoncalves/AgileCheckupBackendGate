package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.PerformanceCycle;
import com.agilecheckup.service.PerformanceCycleService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class PerformanceCycleRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/performancecycles/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/performancecycles/([^/]+)/?$");
  private final PerformanceCycleService performanceCycleService;
  private final ObjectMapper objectMapper;

  public PerformanceCycleRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.performanceCycleService = serviceComponent.buildPerformanceCycleService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /performancecycles
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
      }
      // GET /performancecycles/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /performancecycles
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /performancecycles/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // DELETE /performancecycles/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in performance cycle endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing performance cycle request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(performanceCycleService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<PerformanceCycle> performanceCycle = performanceCycleService.findById(id);

    if (performanceCycle.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(performanceCycle.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Performance cycle not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Date startDate = parseDate(requestMap.get("startDate"));
    Date endDate = parseDate(requestMap.get("endDate"));

    Optional<PerformanceCycle> performanceCycle = performanceCycleService.create(
        (String) requestMap.get("name"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("companyId"),
        (Boolean) requestMap.get("isActive"),
        (Boolean) requestMap.get("isTimeSensitive"),
        startDate,
        endDate
    );

    if (performanceCycle.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(performanceCycle.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create performance cycle");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Date startDate = parseDate(requestMap.get("startDate"));
    Date endDate = parseDate(requestMap.get("endDate"));

    Optional<PerformanceCycle> performanceCycle = performanceCycleService.update(
        id,
        (String) requestMap.get("name"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId"),
        (String) requestMap.get("companyId"),
        (Boolean) requestMap.get("isActive"),
        (Boolean) requestMap.get("isTimeSensitive"),
        startDate,
        endDate
    );

    if (performanceCycle.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(performanceCycle.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Performance cycle not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<PerformanceCycle> performanceCycle = performanceCycleService.findById(id);

    if (performanceCycle.isPresent()) {
      performanceCycleService.delete(performanceCycle.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Performance cycle not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /performancecycles/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }

  private Date parseDate(Object dateValue) throws Exception {
    if (dateValue == null) {
      return null;
    }
    
    if (dateValue instanceof String) {
      String dateString = (String) dateValue;
      if (dateString.isEmpty()) {
        return null;
      }
      // Try ISO 8601 format first (e.g., "2024-01-01T00:00:00.000Z")
      SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      try {
        return isoFormat.parse(dateString);
      } catch (Exception e) {
        // Try simple date format (e.g., "2024-01-01")
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleFormat.parse(dateString);
      }
    } else if (dateValue instanceof Long) {
      // Handle timestamp
      return new Date((Long) dateValue);
    }
    
    throw new IllegalArgumentException("Invalid date format: " + dateValue);
  }
}