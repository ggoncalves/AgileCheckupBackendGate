package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.DaggerServiceComponent;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

public class ApiGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  static {
    System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, RequestHandlerStrategy> routeHandlers;
  private final InvitationRequestHandler invitationHandler;
  private final DashboardAnalyticsRequestHandler dashboardAnalyticsHandler;

  public ApiGatewayHandler() {
    // Initialize your Dagger component
    ServiceComponent serviceComponent = DaggerServiceComponent.create();

    // Initialize route handlers
    this.routeHandlers = new HashMap<>();

    // Register JavaTimeModule to handle LocalDateTime serialization/deserialization
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // Configure Jackson to handle empty strings as null for enums (to support optional Gender/GenderPronoun)
    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    // Register all handlers
    this.routeHandlers.put("companies", new CompanyRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("departments", new DepartmentRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("teams", new TeamRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("performancecycles", new PerformanceCycleRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("assessmentmatrices", new AssessmentMatrixRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("questions", new QuestionRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("answers", new AnswerRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("employeeassessments", new EmployeeAssessmentRequestHandler(serviceComponent, objectMapper));
    this.routeHandlers.put("dashboard-analytics", new DashboardAnalyticsRequestHandler(serviceComponent, objectMapper));
    
    // Initialize special handlers
    this.invitationHandler = new InvitationRequestHandler(serviceComponent, objectMapper);
    this.dashboardAnalyticsHandler = new DashboardAnalyticsRequestHandler(serviceComponent, objectMapper);
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

      // Check for invitation endpoints first
      if (path.contains("/generate-invitation-token") || path.startsWith("/invitation/")) {
        return invitationHandler.handleRequest(input, context);
      }

      // Check for performance cycle summary endpoints
      if (path.startsWith("/performance-cycle-summary/")) {
        return dashboardAnalyticsHandler.handleRequest(input, context);
      }

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