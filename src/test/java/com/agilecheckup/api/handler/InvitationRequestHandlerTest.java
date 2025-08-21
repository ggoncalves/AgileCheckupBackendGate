package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.security.JwtTokenProvider;
import com.agilecheckup.service.AssessmentMatrixService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InvitationRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private AssessmentMatrixService assessmentMatrixService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private InvitationRequestHandler handler;
  private ObjectMapper objectMapper;
  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    jwtTokenProvider = new JwtTokenProvider();
    
    // Setup the service component to return the mocked service
    when(serviceComponent.buildAssessmentMatrixService()).thenReturn(assessmentMatrixService);
    
    handler = new InvitationRequestHandler(serviceComponent, objectMapper);
    lenient().when(context.getLogger()).thenReturn(lambdaLogger);
  }

  @Test
  void shouldGenerateInvitationToken() throws Exception {
    // Given
    String assessmentMatrixId = "matrix-123";
    String tenantId = "tenant-456";
    String requestBody = "{\"tenantId\": \"" + tenantId + "\"}";
    
    // Generate a test token to return from the mocked service
    String expectedToken = jwtTokenProvider.generateInvitationToken(tenantId, assessmentMatrixId);
    when(assessmentMatrixService.generateInvitationToken(anyString(), anyString(), anyBoolean()))
        .thenReturn(expectedToken);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/assessmentmatrices/" + assessmentMatrixId + "/generate-invitation-token").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("token");

    Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
    assertThat(responseBody).containsKey("token");
    assertThat(responseBody.get("token")).isNotBlank();
    assertThat(responseBody.get("token")).isEqualTo(expectedToken);
  }

  @Test
  void shouldReturnBadRequestWhenTenantIdMissing() {
    // Given
    String assessmentMatrixId = "matrix-123";
    String requestBody = "{}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/assessmentmatrices/" + assessmentMatrixId + "/generate-invitation-token").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Tenant ID is required");
  }

  @Test
  void shouldValidateInvitationToken() throws Exception {
    // Given
    String tenantId = "tenant-789";
    String assessmentMatrixId = "matrix-012";

    // Generate a real token using JwtTokenProvider
    JwtTokenProvider tokenProvider = new JwtTokenProvider();
    String token = tokenProvider.generateInvitationToken(tenantId, assessmentMatrixId);

    String requestBody = "{\"token\": \"" + token + "\"}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/invitation/validate-token").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);

    Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
    assertThat(responseBody).containsEntry("tenantId", tenantId);
    assertThat(responseBody).containsEntry("assessmentMatrixId", assessmentMatrixId);
  }

  @Test
  void shouldReturnBadRequestForInvalidToken() {
    // Given
    String requestBody = "{\"token\": \"invalid.jwt.token\"}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/invitation/validate-token").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Invalid or expired invitation link");
  }

  @Test
  void shouldReturnBadRequestWhenTokenMissing() {
    // Given
    String requestBody = "{}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/invitation/validate-token").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Token is required");
  }

  @Test
  void shouldReturn404ForUnknownEndpoint() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/invitation/unknown").withHttpMethod("POST");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Invitation endpoint not found");
  }

  @Test
  void shouldHandleExceptionDuringTokenGeneration() {
    // Given
    String assessmentMatrixId = "matrix-123";
    String invalidRequestBody = "not json";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/assessmentmatrices/" + assessmentMatrixId + "/generate-invitation-token").withHttpMethod("POST").withBody(invalidRequestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Failed to generate invitation token");
  }
}