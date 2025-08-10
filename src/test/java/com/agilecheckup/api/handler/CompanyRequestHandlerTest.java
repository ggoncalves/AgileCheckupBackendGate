package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Company;
import com.agilecheckup.persistency.entity.CompanySize;
import com.agilecheckup.persistency.entity.Industry;
import com.agilecheckup.persistency.entity.person.Address;
import com.agilecheckup.persistency.entity.person.NaturalPerson;
import com.agilecheckup.service.CompanyService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private CompanyService companyService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private CompanyRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    // Configure Jackson to handle empty strings as null for enums (same as ApiGatewayHandler)
    objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    lenient().doReturn(companyService).when(serviceComponent).buildCompanyService();
    lenient().doReturn(lambdaLogger).when(context).getLogger();
    handler = new CompanyRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void shouldSuccessfullyCreateCompanyWithRequiredFields() {
    // Given
    String requestBody = "{\n" +
        "  \"documentNumber\": \"12345678000123\",\n" +
        "  \"name\": \"Tech Company Inc.\",\n" +
        "  \"email\": \"contact@techcompany.com\",\n" +
        "  \"description\": \"A technology company\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"size\": \"STARTUP\",\n" +
        "  \"industry\": \"TECHNOLOGY\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies")
        .withHttpMethod("POST")
        .withBody(requestBody);

    Company createdCompany = Company.builder()
        .id("new-company-id")
        .documentNumber("12345678000123")
        .name("Tech Company Inc.")
        .email("contact@techcompany.com")
        .description("A technology company")
        .tenantId("tenant-123")
        .size(CompanySize.STARTUP)
        .industry(Industry.TECHNOLOGY)
        .build();

    doReturn(Optional.of(createdCompany)).when(companyService).create(
        anyString(), anyString(), anyString(), anyString(), anyString(),
        any(CompanySize.class), any(Industry.class), isNull(), isNull(),
        any(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(companyService).create(
        eq("12345678000123"),
        eq("Tech Company Inc."),
        eq("contact@techcompany.com"),
        eq("A technology company"),
        eq("tenant-123"),
        eq(CompanySize.STARTUP),
        eq(Industry.TECHNOLOGY),
        isNull(),
        isNull(),
        isNull(),
        isNull()
    );

    if (response.getStatusCode() != 201) {
      System.out.println("Expected 201 but got " + response.getStatusCode() + ": " + response.getBody());
    }
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-company-id");
    assertThat(response.getBody()).contains("Tech Company Inc.");
  }

  @Test
  void shouldSuccessfullyCreateCompanyWithAllFields() {
    // Given
    String requestBody = "{\n" +
        "  \"documentNumber\": \"98765432000198\",\n" +
        "  \"name\": \"Full Company Ltd.\",\n" +
        "  \"email\": \"info@fullcompany.com\",\n" +
        "  \"description\": \"A complete company\",\n" +
        "  \"tenantId\": \"tenant-456\",\n" +
        "  \"size\": \"LARGE\",\n" +
        "  \"industry\": \"FINANCE\",\n" +
        "  \"website\": \"https://www.fullcompany.com\",\n" +
        "  \"legalName\": \"Full Company Legal Ltd.\",\n" +
        "  \"contactPerson\": {\n" +
        "    \"name\": \"John Doe\",\n" +
        "    \"email\": \"john.doe@fullcompany.com\",\n" +
        "    \"documentNumber\": \"12345678901\"\n" +
        "  },\n" +
        "  \"address\": {\n" +
        "    \"street\": \"123 Business Ave\",\n" +
        "    \"city\": \"São Paulo\",\n" +
        "    \"state\": \"SP\",\n" +
        "    \"zipcode\": \"01234-567\",\n" +
        "    \"country\": \"Brazil\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies")
        .withHttpMethod("POST")
        .withBody(requestBody);

    Company createdCompany = Company.builder()
        .id("full-company-id")
        .documentNumber("98765432000198")
        .name("Full Company Ltd.")
        .email("info@fullcompany.com")
        .description("A complete company")
        .tenantId("tenant-456")
        .size(CompanySize.LARGE)
        .industry(Industry.FINANCE)
        .website("https://www.fullcompany.com")
        .legalName("Full Company Legal Ltd.")
        .build();

    ArgumentCaptor<NaturalPerson> contactPersonCaptor = ArgumentCaptor.forClass(NaturalPerson.class);
    ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

    doReturn(Optional.of(createdCompany)).when(companyService).create(
        anyString(), anyString(), anyString(), anyString(), anyString(),
        any(CompanySize.class), any(Industry.class), anyString(), anyString(),
        any(NaturalPerson.class), any(Address.class));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(companyService).create(
        eq("98765432000198"),
        eq("Full Company Ltd."),
        eq("info@fullcompany.com"),
        eq("A complete company"),
        eq("tenant-456"),
        eq(CompanySize.LARGE),
        eq(Industry.FINANCE),
        eq("https://www.fullcompany.com"),
        eq("Full Company Legal Ltd."),
        contactPersonCaptor.capture(),
        addressCaptor.capture()
    );

    // Verify contact person was parsed correctly
    NaturalPerson capturedContactPerson = contactPersonCaptor.getValue();
    assertThat(capturedContactPerson.getName()).isEqualTo("John Doe");
    assertThat(capturedContactPerson.getEmail()).isEqualTo("john.doe@fullcompany.com");

    // Verify address was parsed correctly
    Address capturedAddress = addressCaptor.getValue();
    assertThat(capturedAddress.getStreet()).isEqualTo("123 Business Ave");
    assertThat(capturedAddress.getCity()).isEqualTo("São Paulo");
    assertThat(capturedAddress.getState()).isEqualTo("SP");

    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("full-company-id");
  }

  @Test
  void shouldReturnBadRequestWhenRequiredFieldsMissing() {
    // Given
    String requestBody = "{\n" +
        "  \"documentNumber\": \"12345678000123\",\n" +
        "  \"name\": \"Tech Company Inc.\",\n" +
        "  \"email\": \"contact@techcompany.com\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Description is required, Tenant ID is required, Company size is required, Industry is required");
    verify(companyService, never()).create(anyString(), anyString(), anyString(), anyString(), anyString(),
        any(CompanySize.class), any(Industry.class), anyString(), anyString(), any(), any());
  }

  @Test
  void shouldReturnBadRequestWhenInvalidCompanySize() {
    // Given
    String requestBody = "{\n" +
        "  \"documentNumber\": \"12345678000123\",\n" +
        "  \"name\": \"Tech Company Inc.\",\n" +
        "  \"email\": \"contact@techcompany.com\",\n" +
        "  \"description\": \"A technology company\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"size\": \"INVALID_SIZE\",\n" +
        "  \"industry\": \"TECHNOLOGY\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Validation error");
    assertThat(response.getBody()).contains("Invalid company size");
  }

  @Test
  void shouldReturnBadRequestWhenInvalidIndustry() {
    // Given
    String requestBody = "{\n" +
        "  \"documentNumber\": \"12345678000123\",\n" +
        "  \"name\": \"Tech Company Inc.\",\n" +
        "  \"email\": \"contact@techcompany.com\",\n" +
        "  \"description\": \"A technology company\",\n" +
        "  \"tenantId\": \"tenant-123\",\n" +
        "  \"size\": \"STARTUP\",\n" +
        "  \"industry\": \"INVALID_INDUSTRY\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Validation error");
    assertThat(response.getBody()).contains("Invalid industry");
  }

  @Test
  void shouldSuccessfullyUpdateCompanyWithAddressAndContactPerson() {
    // Given
    String companyId = "existing-company-id";
    String requestBody = "{\n" +
        "  \"documentNumber\": \"11111111000111\",\n" +
        "  \"name\": \"Updated Company\",\n" +
        "  \"email\": \"updated@company.com\",\n" +
        "  \"description\": \"Updated description\",\n" +
        "  \"tenantId\": \"tenant-789\",\n" +
        "  \"size\": \"MEDIUM\",\n" +
        "  \"industry\": \"HEALTHCARE\",\n" +
        "  \"website\": \"https://www.updated.com\",\n" +
        "  \"legalName\": \"Updated Company Legal\",\n" +
        "  \"phone\": \"1234567890\",\n" +
        "  \"address\": {\n" +
        "    \"street\": \"123 Main St\",\n" +
        "    \"city\": \"New York\",\n" +
        "    \"state\": \"NY\",\n" +
        "    \"zipcode\": \"10001\",\n" +
        "    \"country\": \"USA\"\n" +
        "  },\n" +
        "  \"contactPerson\": {\n" +
        "    \"id\": \"person-123\",\n" +
        "    \"name\": \"John\",\n" +
//        "    \"lastName\": \"Doe\",\n" +
        "    \"email\": \"john.doe@example.com\",\n" +
        "    \"phone\": \"9876543210\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withHttpMethod("PUT")
        .withPath("/companies/" + companyId)
        .withBody(requestBody);

    Company existingCompany = new Company();
    existingCompany.setId(companyId);

    Company updatedCompany = new Company();
    updatedCompany.setId(companyId);
    updatedCompany.setDocumentNumber("11111111000111");
    updatedCompany.setName("Updated Company");
    updatedCompany.setEmail("updated@company.com");
    updatedCompany.setDescription("Updated description");
    updatedCompany.setTenantId("tenant-789");
    updatedCompany.setSize(CompanySize.MEDIUM);
    updatedCompany.setIndustry(Industry.HEALTHCARE);
    updatedCompany.setWebsite("https://www.updated.com");
    updatedCompany.setLegalName("Updated Company Legal");
    updatedCompany.setPhone("1234567890");

    // Set up Address
    Address address = new Address();
    address.setStreet("123 Main St");
    address.setCity("New York");
    address.setState("NY");
    address.setZipcode("10001");
    address.setCountry("USA");
    updatedCompany.setAddress(address);

    // Set up NaturalPerson
    NaturalPerson contactPerson = new NaturalPerson();
    contactPerson.setId("person-123");
    contactPerson.setName("John");
//    contactPerson.setLastName("Doe");
    contactPerson.setEmail("john.doe@example.com");
    contactPerson.setPhone("9876543210");
    updatedCompany.setContactPerson(contactPerson);

    doReturn(Optional.of(existingCompany)).when(companyService).update(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(CompanySize.class),
        any(Industry.class), any(), any(), any(), any());


    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);

    // Capture the argument passed to update method
    ArgumentCaptor<NaturalPerson> contactPersonCaptor = ArgumentCaptor.forClass(NaturalPerson.class);
    ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

    verify(companyService).update(
        eq(companyId),
        eq("11111111000111"),
        eq("Updated Company"),
        eq("updated@company.com"),
        eq("Updated description"),
        eq("tenant-789"),
        eq(CompanySize.MEDIUM),
        eq(Industry.HEALTHCARE),
        eq("https://www.updated.com"),
        eq("Updated Company Legal"),
        contactPersonCaptor.capture(),
        addressCaptor.capture()
    );

    // Verify Address fields
    Address capturedAddress = addressCaptor.getValue();
    assertThat(capturedAddress).isNotNull();
    assertThat(capturedAddress.getStreet()).isEqualTo("123 Main St");
    assertThat(capturedAddress.getCity()).isEqualTo("New York");
    assertThat(capturedAddress.getState()).isEqualTo("NY");
    assertThat(capturedAddress.getZipcode()).isEqualTo("10001");
    assertThat(capturedAddress.getCountry()).isEqualTo("USA");

    // Verify ContactPerson fields
    NaturalPerson capturedContactPerson = contactPersonCaptor.getValue();
    assertThat(capturedContactPerson).isNotNull();
    assertThat(capturedContactPerson.getId()).isEqualTo("person-123");
    assertThat(capturedContactPerson.getName()).isEqualTo("John");
    assertThat(capturedContactPerson.getEmail()).isEqualTo("john.doe@example.com");
    assertThat(capturedContactPerson.getPhone()).isEqualTo("9876543210");
  }

  @Test
  void shouldSuccessfullyUpdateCompanyWithAllFields() {
    // Given
    String companyId = "existing-company-id";
    String requestBody = "{\n" +
        "  \"documentNumber\": \"11111111000111\",\n" +
        "  \"name\": \"Updated Company\",\n" +
        "  \"email\": \"updated@company.com\",\n" +
        "  \"description\": \"Updated description\",\n" +
        "  \"tenantId\": \"tenant-789\",\n" +
        "  \"size\": \"MEDIUM\",\n" +
        "  \"industry\": \"HEALTHCARE\",\n" +
        "  \"website\": \"https://www.updated.com\",\n" +
        "  \"legalName\": \"Updated Company Legal\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies/" + companyId)
        .withHttpMethod("PUT")
        .withBody(requestBody);

    Company updatedCompany = Company.builder()
        .id(companyId)
        .documentNumber("11111111000111")
        .name("Updated Company")
        .email("updated@company.com")
        .description("Updated description")
        .tenantId("tenant-789")
        .size(CompanySize.MEDIUM)
        .industry(Industry.HEALTHCARE)
        .website("https://www.updated.com")
        .legalName("Updated Company Legal")
        .build();

    doReturn(Optional.of(updatedCompany)).when(companyService).update(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
        any(CompanySize.class), any(Industry.class), anyString(), anyString(),
        any(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(companyService).update(
        eq(companyId),
        eq("11111111000111"),
        eq("Updated Company"),
        eq("updated@company.com"),
        eq("Updated description"),
        eq("tenant-789"),
        eq(CompanySize.MEDIUM),
        eq(Industry.HEALTHCARE),
        eq("https://www.updated.com"),
        eq("Updated Company Legal"),
        isNull(),
        isNull()
    );

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("Updated Company");
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingNonExistentCompany() {
    // Given
    String companyId = "non-existent-id";
    String requestBody = "{\n" +
        "  \"documentNumber\": \"11111111000111\",\n" +
        "  \"name\": \"Updated Company\",\n" +
        "  \"email\": \"updated@company.com\",\n" +
        "  \"description\": \"Updated description\",\n" +
        "  \"tenantId\": \"tenant-789\",\n" +
        "  \"size\": \"MEDIUM\",\n" +
        "  \"industry\": \"HEALTHCARE\"\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies/" + companyId)
        .withHttpMethod("PUT")
        .withBody(requestBody);

    doReturn(Optional.empty()).when(companyService).update(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(CompanySize.class),
        any(Industry.class), isNull(), isNull(), any(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Company not found or update failed");
  }

  @Test
  void shouldSuccessfullyUpdateCompanyWithEmptyGenderFields() {
    // Given - This test reproduces the bug where empty strings for gender cause Jackson deserialization to fail
    String companyId = "existing-company-id";
    String requestBody = "{\n" +
        "  \"name\": \"New Company without Optional Fields\",\n" +
        "  \"email\": \"glauciocgoncalves@gmail.com\",\n" +
        "  \"tenantId\": \"new-company-tenant\",\n" +
        "  \"documentNumber\": \"74.790.229/0001-03\",\n" +
        "  \"description\": \"New Company without Optional Fields\",\n" +
        "  \"size\": \"STARTUP\",\n" +
        "  \"industry\": \"NONPROFIT\",\n" +
        "  \"website\": \"\",\n" +
        "  \"legalName\": \"\",\n" +
        "  \"phone\": \"\",\n" +
        "  \"contactPerson\": {\n" +
        "    \"id\": \"55de44ae-fd83-45cf-be3d-665fbeca0ee3\",\n" +
        "    \"createdDate\": null,\n" +
        "    \"lastUpdatedDate\": null,\n" +
        "    \"name\": \"Carla\",\n" +
        "    \"email\": \"carla@gmail.com\",\n" +
        "    \"phone\": \"\",\n" +
        "    \"address\": null,\n" +
        "    \"personDocumentType\": \"CPF\",\n" +
        "    \"documentNumber\": \"\",\n" +
        "    \"aliasName\": null,\n" +
        "    \"gender\": \"\",\n" +
        "    \"genderPronoun\": \"\"\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/companies/" + companyId)
        .withHttpMethod("PUT")
        .withBody(requestBody);

    Company updatedCompany = Company.builder()
        .id(companyId)
        .name("New Company without Optional Fields")
        .email("glauciocgoncalves@gmail.com")
        .tenantId("new-company-tenant")
        .documentNumber("74.790.229/0001-03")
        .description("New Company without Optional Fields")
        .size(CompanySize.STARTUP)
        .industry(Industry.NONPROFIT)
        .website("")
        .legalName("")
        .build();

    doReturn(Optional.of(updatedCompany)).when(companyService).update(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
        any(CompanySize.class), any(Industry.class), anyString(), anyString(),
        any(), any());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("New Company without Optional Fields");

    // Verify that the contactPerson with empty gender fields was processed correctly
    ArgumentCaptor<NaturalPerson> contactPersonCaptor = ArgumentCaptor.forClass(NaturalPerson.class);
    verify(companyService).update(
        eq(companyId),
        eq("74.790.229/0001-03"),
        eq("New Company without Optional Fields"),
        eq("glauciocgoncalves@gmail.com"),
        eq("New Company without Optional Fields"),
        eq("new-company-tenant"),
        eq(CompanySize.STARTUP),
        eq(Industry.NONPROFIT),
        eq(""),
        eq(""),
        contactPersonCaptor.capture(),
        any());

    NaturalPerson capturedContactPerson = contactPersonCaptor.getValue();
    assertThat(capturedContactPerson.getName()).isEqualTo("Carla");
    assertThat(capturedContactPerson.getEmail()).isEqualTo("carla@gmail.com");
    assertThat(capturedContactPerson.getGender()).isNull(); // Empty string should be converted to null
    assertThat(capturedContactPerson.getGenderPronoun()).isNull(); // Empty string should be converted to null
  }
}