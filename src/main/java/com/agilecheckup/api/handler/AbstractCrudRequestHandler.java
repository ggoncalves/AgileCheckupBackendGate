package com.agilecheckup.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class AbstractCrudRequestHandler<T> implements RequestHandlerStrategy {

    protected final ObjectMapper objectMapper;
    private final Pattern getAllPattern;
    private final Pattern singleResourcePattern;
    
    protected AbstractCrudRequestHandler(ObjectMapper objectMapper, String resourcePath) {
        this.objectMapper = objectMapper;
        this.getAllPattern = Pattern.compile("^/" + resourcePath + "/?$");
        this.singleResourcePattern = Pattern.compile("^/" + resourcePath + "/([^/]+)/?$");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String path = input.getPath();
            String method = input.getHttpMethod();

            // Check for custom endpoints first
            Optional<APIGatewayProxyResponseEvent> customResponse = handleCustomEndpoint(method, path, input, context);
            if (customResponse.isPresent()) {
                return customResponse.get();
            }

            // Standard CRUD operations
            if (method.equals("GET") && getAllPattern.matcher(path).matches()) {
                return handleGetAll(input);
            } else if (method.equals("GET") && singleResourcePattern.matcher(path).matches()) {
                String id = extractIdFromPath(path);
                return handleGetById(id);
            } else if (method.equals("POST") && getAllPattern.matcher(path).matches()) {
                return handleCreate(input.getBody(), context);
            } else if (method.equals("PUT") && singleResourcePattern.matcher(path).matches()) {
                String id = extractIdFromPath(path);
                return handleUpdate(id, input.getBody(), context);
            } else if (method.equals("DELETE") && singleResourcePattern.matcher(path).matches()) {
                String id = extractIdFromPath(path);
                return handleDelete(id);
            } else {
                return ResponseBuilder.buildResponse(405, "Method Not Allowed");
            }

        } catch (IllegalArgumentException e) {
            context.getLogger().log("Validation error in " + getResourceName() + " endpoint: " + e.getMessage());
            return ResponseBuilder.buildResponse(400, "Validation error: " + e.getMessage());
        } catch (Exception e) {
            context.getLogger().log("Error in " + getResourceName() + " endpoint: " + e.getMessage());
            return ResponseBuilder.buildResponse(500, "Error processing " + getResourceName() + " request: " + e.getMessage());
        }
    }

    // Abstract methods for CRUD operations
    protected abstract APIGatewayProxyResponseEvent handleGetAll(APIGatewayProxyRequestEvent input) throws Exception;
    protected abstract APIGatewayProxyResponseEvent handleGetById(String id) throws Exception;
    protected abstract APIGatewayProxyResponseEvent handleCreate(String requestBody, Context context) throws Exception;
    protected abstract APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody, Context context) throws Exception;
    protected abstract APIGatewayProxyResponseEvent handleDelete(String id) throws Exception;
    
    // Hook for custom endpoints - default returns empty
    protected Optional<APIGatewayProxyResponseEvent> handleCustomEndpoint(String method, String path, 
                                                                         APIGatewayProxyRequestEvent input, 
                                                                         Context context) throws Exception {
        return Optional.empty();
    }
    
    // Helper method for resource name (for error messages)
    protected abstract String getResourceName();
    
    // Common utility methods
    protected String extractIdFromPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
    
    // Common response helpers
    protected APIGatewayProxyResponseEvent buildNotFoundResponse() {
        return ResponseBuilder.buildResponse(404, getResourceName() + " not found");
    }
    
    protected APIGatewayProxyResponseEvent buildCreationFailedResponse() {
        return ResponseBuilder.buildResponse(400, "Failed to create " + getResourceName());
    }
    
    protected APIGatewayProxyResponseEvent buildUpdateFailedResponse() {
        return ResponseBuilder.buildResponse(404, getResourceName() + " not found or update failed");
    }
}