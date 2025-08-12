package com.agilecheckup.api;

import com.agilecheckup.api.handler.ApiGatewayHandler;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public class LocalRunner {
  public static void main(String[] args) {
    // Create the handler
    ApiGatewayHandler handler = new ApiGatewayHandler();

    // Create a sample request for getting companies
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setHttpMethod("GET");
    request.setPath("/companies");

    // Create a basic mock context
    Context context = new Context() {
      @Override
      public String getAwsRequestId() {
        return "testId";
      }

      @Override
      public String getLogGroupName() {
        return "testGroup";
      }

      @Override
      public String getLogStreamName() {
        return "testStream";
      }

      @Override
      public String getFunctionName() {
        return "testFunction";
      }

      @Override
      public String getFunctionVersion() {
        return "testVersion";
      }

      @Override
      public String getInvokedFunctionArn() {
        return "testArn";
      }

      @Override
      public LambdaLogger getLogger() {
        return new LambdaLogger() {
          @Override
          public void log(String message) {
            System.out.println(message);
          }

          @Override
          public void log(byte[] message) {
            System.out.println(new String(message));
          }
        };
      }

      @Override
      public int getRemainingTimeInMillis() {
        return 30000;
      }

      @Override
      public int getMemoryLimitInMB() {
        return 128;
      }

      @Override
      public ClientContext getClientContext() {
        return null;
      }

      @Override
      public CognitoIdentity getIdentity() {
        return null;
      }
    };

    // Call the handler
    var response = handler.handleRequest(request, context);

    // Print the response
    System.out.println("Status: " + response.getStatusCode());
    System.out.println("Body: " + response.getBody());
  }
}