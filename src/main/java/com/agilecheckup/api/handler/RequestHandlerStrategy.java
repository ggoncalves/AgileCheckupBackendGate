package com.agilecheckup.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public interface RequestHandlerStrategy {
  APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context);
}