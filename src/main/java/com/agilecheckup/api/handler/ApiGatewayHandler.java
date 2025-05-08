package com.agilecheckup.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.agilecheckup.dagger.component.DaggerServiceComponent;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ApiGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final ServiceComponent serviceComponent;
  private final Map<String, RequestHandlerStrategy> routeHandlers;

  public ApiGatewayHandler() {
    // Initialize your Dagger component
    this.serviceComponent = DaggerServiceComponent.create();

    // Initialize route handlers
    this.routeHandlers = new HashMap<>();
    this.routeHandlers.put("companies", new CompanyRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("departments", new DepartmentRequestHandler(serviceComponent, objectMapper));
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      // Log request info
      context.getLogger().log("Received event: " + input.getPath() + " " + input.getHttpMethod());

      // Parse the request path
      String path = input.getPath();
      if (path == null || path.isEmpty()) {
        return ResponseBuilder.buildResponse(404, "Not Found");
      }

      // Remove leading slash and get first path segment
      String[] pathSegments = path.replaceAll("^/", "").split("/");
      if (pathSegments.length == 0) {
        return ResponseBuilder.buildResponse(404, "Not Found");
      }

      String resourceType = pathSegments[0];

      // Delegate to the appropriate handler based on resource type
      RequestHandlerStrategy handler = routeHandlers.get(resourceType);
      if (handler != null) {
        return handler.handleRequest(input, context);
      }

      // No handler found for this path
      return ResponseBuilder.buildResponse(404, "Resource Not Found");

    } catch (Exception e) {
      context.getLogger().log("Error processing request: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Internal Server Error: " + e.getMessage());
    }
  }
}