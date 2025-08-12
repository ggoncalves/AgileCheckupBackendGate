package com.agilecheckup.api.handler;

import com.agilecheckup.api.validator.CompanyValidator;
import com.agilecheckup.api.validator.ValidationResult;
import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Company;
import com.agilecheckup.persistency.entity.CompanySize;
import com.agilecheckup.persistency.entity.Industry;
import com.agilecheckup.persistency.entity.person.Address;
import com.agilecheckup.persistency.entity.person.Gender;
import com.agilecheckup.persistency.entity.person.GenderPronoun;
import com.agilecheckup.persistency.entity.person.NaturalPerson;
import com.agilecheckup.persistency.entity.person.PersonDocumentType;
import com.agilecheckup.service.CompanyService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    } catch (IllegalArgumentException e) {
      context.getLogger().log("Validation error in company endpoint: " + e.getMessage());
      return ResponseBuilder.buildResponse(400, "Validation error: " + e.getMessage());
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
    com.agilecheckup.api.model.Company companyBody = parseCompanyRequest(requestBody);

    ValidationResult result = CompanyValidator.validate(companyBody);

    if (!result.isValid()) {
      return ResponseBuilder.buildResponse(400, result.getErrorMessage());
    }

    CompanySize size = parseCompanySize(companyBody.getSize());
    Industry industry = parseIndustry(companyBody.getIndustry());

    // Convert API DTOs to  entities
    NaturalPerson contactPerson = convertDtoToNaturalPerson(companyBody.getContactPerson());
    Address address = convertDtoToAddress(companyBody.getAddress());

    Optional<Company> company = companyService.create(
        companyBody.getDocumentNumber(),
        companyBody.getName(),
        companyBody.getEmail(),
        companyBody.getDescription(),
        companyBody.getTenantId(),
        size,
        industry,
        companyBody.getWebsite(),
        companyBody.getLegalName(),
        contactPerson,
        address
    );

    if (company.isPresent()) {
      return ResponseBuilder.buildResponse(201, objectMapper.writeValueAsString(company.get()));
    } else {
      return ResponseBuilder.buildResponse(400, "Failed to create company");
    }
  }

  private APIGatewayProxyResponseEvent handleUpdate(String id, String requestBody) throws Exception {
    com.agilecheckup.api.model.Company companyBody = parseCompanyRequest(requestBody);

    ValidationResult result = CompanyValidator.validate(companyBody);

    if (!result.isValid()) {
      return ResponseBuilder.buildResponse(400, result.getErrorMessage());
    }

    CompanySize size = parseCompanySize(companyBody.getSize());
    Industry industry = parseIndustry(companyBody.getIndustry());

    // Convert API DTOs to  entities
    NaturalPerson contactPerson = convertDtoToNaturalPerson(companyBody.getContactPerson());
    Address address = convertDtoToAddress(companyBody.getAddress());

    // Use update method
    Optional<Company> company = companyService.update(
        id,
        companyBody.getDocumentNumber(),
        companyBody.getName(),
        companyBody.getEmail(),
        companyBody.getDescription(),
        companyBody.getTenantId(),
        size,
        industry,
        companyBody.getWebsite(),
        companyBody.getLegalName(),
        contactPerson,
        address
    );

    if (company.isPresent()) {
      return ResponseBuilder.buildResponse(200, objectMapper.writeValueAsString(company.get()));
    } else {
      return ResponseBuilder.buildResponse(404, "Company not found or update failed");
    }
  }

  private com.agilecheckup.api.model.Company parseCompanyRequest(String requestBody) throws JsonProcessingException {
    return objectMapper.readValue(requestBody, com.agilecheckup.api.model.Company.class);
  }

  private APIGatewayProxyResponseEvent handleDelete(String id) {
    Optional<Company> company = companyService.findById(id);

    if (company.isPresent()) {
      companyService.deleteById(id);
      return ResponseBuilder.buildResponse(204, "");
    } else {
      return ResponseBuilder.buildResponse(404, "Company not found");
    }
  }

  private String extractIdFromPath(String path) {
    // Extract ID from path like /companies/{id}
    return path.substring(path.lastIndexOf("/") + 1);
  }

  private CompanySize parseCompanySize(String sizeStr) {
    if (sizeStr == null || sizeStr.trim().isEmpty()) {
      return null;
    }
    try {
      return CompanySize.valueOf(sizeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid company size: " + sizeStr +
          ". Valid values are: STARTUP, SMALL, MEDIUM, LARGE, ENTERPRISE");
    }
  }

  private Industry parseIndustry(String industryStr) {
    if (industryStr == null || industryStr.trim().isEmpty()) {
      return null;
    }
    try {
      return Industry.valueOf(industryStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid industry: " + industryStr +
          ". Valid values are: TECHNOLOGY, FINANCE, HEALTHCARE, MANUFACTURING, RETAIL, EDUCATION, CONSULTING, GOVERNMENT, NONPROFIT, OTHER");
    }
  }

  private NaturalPerson convertDtoToNaturalPerson(com.agilecheckup.api.model.NaturalPersonDto dto) {
    if (dto == null) {
      return null;
    }
    
    // Parse enums from string
    PersonDocumentType docType = null;
    if (dto.getPersonDocumentType() != null && !dto.getPersonDocumentType().trim().isEmpty()) {
      try {
        docType = PersonDocumentType.valueOf(dto.getPersonDocumentType());
      } catch (IllegalArgumentException e) {
        // Log warning but continue
      }
    }
    
    Gender gender = null;
    if (dto.getGender() != null && !dto.getGender().trim().isEmpty()) {
      try {
        gender = Gender.valueOf(dto.getGender());
      } catch (IllegalArgumentException e) {
        // Log warning but continue
      }
    }
    
    GenderPronoun genderPronoun = null;
    if (dto.getGenderPronoun() != null && !dto.getGenderPronoun().trim().isEmpty()) {
      try {
        genderPronoun = GenderPronoun.valueOf(dto.getGenderPronoun());
      } catch (IllegalArgumentException e) {
        // Log warning but continue
      }
    }
    
    return NaturalPerson.builder()
        .id(dto.getId())
        .name(dto.getName())
        .email(dto.getEmail())
        .phone(dto.getPhone())
        .documentNumber(dto.getDocumentNumber())
        .personDocumentType(docType)
        .aliasName(dto.getAliasName())
        .gender(gender)
        .genderPronoun(genderPronoun)
        .address(convertDtoToAddress(dto.getAddress()))
        .build();
  }

  private Address convertDtoToAddress(com.agilecheckup.api.model.AddressDto dto) {
    if (dto == null) {
      return null;
    }
    return Address.builder()
        .id(dto.getId())
        .street(dto.getStreet())
        .city(dto.getCity())
        .state(dto.getState())
        .country(dto.getCountry())
        .zipcode(dto.getZipcode())
        .build();
  }
}