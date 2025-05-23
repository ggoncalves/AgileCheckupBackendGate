package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Company;
import com.agilecheckup.service.CompanyService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class CompanyRequestHandler implements RequestHandlerStrategy {

  // Regex patterns for path matching
  private static final Pattern GET_ALL_PATTERN = Pattern.compile("^/companies/?$");
  private static final Pattern SINGLE_RESOURCE_PATTERN = Pattern.compile("^/companies/([^/]+)/?$");
  private final CompanyService companyService;
  private final ObjectMapper objectMapper;

  public CompanyRequestHandler(ServiceComponent serviceComponent, ObjectMapper objectMapper) {
    this.companyService = serviceComponent.buildCompanyService();
    this.objectMapper = objectMapper;
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    try {
      String path = input.getPath();
      String method = input.getHttpMethod();

      // GET /companies
      if (method.equals("GET") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleGetAll();
      }
      // GET /companies/{id}
      else if (method.equals("GET") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleGetById(id);
      }
      // POST /companies
      else if (method.equals("POST") && GET_ALL_PATTERN.matcher(path).matches()) {
        return handleCreate(input.getBody());
      }
      // PUT /companies/{id}
      else if (method.equals("PUT") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleUpdate(id, input.getBody());
      }
      // DELETE /companies/{id}
      else if (method.equals("DELETE") && SINGLE_RESOURCE_PATTERN.matcher(path).matches()) {
        String id = extractIdFromPath(path);
        return handleDelete(id);
      }
      // Method not supported
      else {
        return ResponseBuilder.buildResponse(405, "Method Not Allowed");
      }

    } catch (Exception e) {
      context.getLogger().log("Error in company endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(500, "Error processing company request: " + e.getMessage());
    }
  }

  private APIGatewayProxyResponseEvent handleGetAll() throws Exception {
    return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(companyService.findAll()));
  }

  private APIGatewayProxyResponseEvent handleGetById(String id) throws Exception {
    Optional<Company> company = companyService.findById(id);

    if (company.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(company.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Company not found");
    }
  }

  private APIGatewayProxyResponseEvent handleCreate(String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Company> company = companyService.create(
        (String) requestMap.get("documentNumber"),
        (String) requestMap.get("name"),
        (String) requestMap.get("email"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId")
    );

    if (company.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(company.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create company");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    Map<String, Object> requestMap = objectMapper.readValue(requestBody, Map.class);

    Optional<Company> company = companyService.update(
        id,
        (String) requestMap.get("documentNumber"),
        (String) requestMap.get("name"),
        (String) requestMap.get("email"),
        (String) requestMap.get("description"),
        (String) requestMap.get("tenantId")
    );

    if (company.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(company.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Company not found or update failed");
    }
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Company> company = companyService.findById(id);

    if (company.isPresent()) {
      companyService.delete(company.get());
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Company not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /companies/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }
}