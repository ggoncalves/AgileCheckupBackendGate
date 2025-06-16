package com.agilecheckup.api.handler;

import com.agilecheckup.security.JwtTokenProvider;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InvitationRequestHandler {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public InvitationRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = new JwtTokenProvider();
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String path = request.getPath();
        String method = request.getHttpMethod();
        
        log.info("Processing invitation request: {} {}", method, path);

        try {
            // Generate invitation token
            if (path.matches("/assessmentmatrices/[^/]+/generate-invitation-token") && "POST".equals(method)) {
                return generateInvitationToken(request);
            }
            
            // Validate invitation token
            if (path.equals("/invitation/validate-token") && "POST".equals(method)) {
                return validateInvitationToken(request);
            }

            return ResponseBuilder.buildResponse(404, "Invitation endpoint not found");
            
        } catch (Exception e) {
            log.error("Error processing invitation request: {}", e.getMessage(), e);
            return ResponseBuilder.buildResponse(500, "Error processing invitation request");
        }
    }

    private APIGatewayProxyResponseEvent generateInvitationToken(APIGatewayProxyRequestEvent request) {
        try {
            // Extract assessment matrix ID from path
            String path = request.getPath();
            String assessmentMatrixId = path.split("/")[2]; // /assessmentmatrices/{id}/generate-invitation-token
            
            // Parse request body to get tenant ID
            Map<String, Object> requestBody = objectMapper.readValue(request.getBody(), Map.class);
            String tenantId = (String) requestBody.get("tenantId");
            
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return ResponseBuilder.buildResponse(400, "Tenant ID is required");
            }
            
            // Generate JWT token
            String token = jwtTokenProvider.generateInvitationToken(tenantId, assessmentMatrixId);
            
            // Return token
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            
            return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));
            
        } catch (Exception e) {
            log.error("Error generating invitation token: {}", e.getMessage(), e);
            return ResponseBuilder.buildResponse(500, "Failed to generate invitation token");
        }
    }

    private APIGatewayProxyResponseEvent validateInvitationToken(APIGatewayProxyRequestEvent request) {
        try {
            // Parse request body to get token
            Map<String, Object> requestBody = objectMapper.readValue(request.getBody(), Map.class);
            String token = (String) requestBody.get("token");
            
            if (token == null || token.trim().isEmpty()) {
                return ResponseBuilder.buildResponse(400, "Token is required");
            }
            
            // Validate and parse token
            Claims claims = jwtTokenProvider.validateAndParseToken(token);
            
            // Extract data from token
            Map<String, String> response = new HashMap<>();
            response.put("tenantId", claims.get("tenantId", String.class));
            response.put("assessmentMatrixId", claims.get("assessmentMatrixId", String.class));
            
            return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(response));
            
        } catch (Exception e) {
            log.error("Error validating invitation token: {}", e.getMessage());
            return ResponseBuilder.buildResponse(400, "Invalid or expired invitation link");
        }
    }
}