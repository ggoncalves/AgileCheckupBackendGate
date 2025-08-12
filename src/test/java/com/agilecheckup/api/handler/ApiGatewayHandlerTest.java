package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

class ApiGatewayHandlerTest {

  private ApiGatewayHandler handler;

  @Mock
  private Context context;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new ApiGatewayHandler();

    // Set up context mock
    doReturn(new TestLogger()).when(context).getLogger();
  }

  @Test
  void handleRequest_unknownPath_returns404() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/unknown");
    request.setHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Not Found");
  }

  @Test
  void handleRequest_invitationTokenGeneration_routesToInvitationHandler() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/assessmentmatrices/matrix-123/generate-invitation-token");
    request.setHttpMethod("POST");
    request.setBody("{\"tenantId\": \"tenant-456\"}");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("token");
  }

  @Test
  void handleRequest_invitationTokenValidation_routesToInvitationHandler() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/invitation/validate-token");
    request.setHttpMethod("POST");
    request.setBody("{\"token\": \"invalid.token\"}");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    // Should route to invitation handler (not return 404 which means no routing happened)
    assertThat(response.getStatusCode()).isNotEqualTo(404);
  }

  @Test
  void handleRequest_companiesEndpoint_routesToCompanyHandler() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setPath("/companies");
    request.setHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    // Should route to company handler (not return 404 which means no routing happened)
    assertThat(response.getStatusCode()).isNotEqualTo(404);
  }

  // Helper class for mocking the Lambda logger
  private static class TestLogger implements com.amazonaws.services.lambda.runtime.LambdaLogger {
    @Override
    public void log(String message) {
      System.out.println(message);
    }

    @Override
    public void log(byte[] message) {
      System.out.println(new String(message));
    }
  }
}