package com.agilecheckup.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

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