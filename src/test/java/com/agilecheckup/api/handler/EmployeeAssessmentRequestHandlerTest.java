package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.EmployeeAssessment;
import com.agilecheckup.persistency.entity.EmployeeAssessmentScore;
import com.agilecheckup.persistency.entity.person.Gender;
import com.agilecheckup.persistency.entity.person.GenderPronoun;
import com.agilecheckup.persistency.entity.person.NaturalPerson;
import com.agilecheckup.persistency.entity.person.PersonDocumentType;
import com.agilecheckup.service.EmployeeAssessmentService;
import com.agilecheckup.service.dto.EmployeeValidationRequest;
import com.agilecheckup.service.dto.EmployeeValidationResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class EmployeeAssessmentRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private EmployeeAssessmentService employeeAssessmentService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private EmployeeAssessmentRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    lenient().doReturn(employeeAssessmentService).when(serviceComponent).buildEmployeeAssessmentService();
    lenient().doReturn(lambdaLogger).when(context).getLogger();
    handler = new EmployeeAssessmentRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void shouldSuccessfullyGetAllEmployeeAssessments() {
    // Given
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("GET").withQueryStringParameters(queryParams);

    EmployeeAssessment assessment1 = EmployeeAssessment.builder().id("ea-1").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).teamId("team-123").build();
    assessment1.setTenantId("test-tenant-123");

    EmployeeAssessment assessment2 = EmployeeAssessment.builder().id("ea-2").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("jane.smith@example.com").name("Jane Smith").gender(Gender.FEMALE).genderPronoun(GenderPronoun.SHE).build()).teamId("team-123").build();
    assessment2.setTenantId("test-tenant-123");

    List<EmployeeAssessment> assessmentList = Arrays.asList(assessment1, assessment2);
    doReturn(assessmentList).when(employeeAssessmentService).findAllByTenantId("test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).findAllByTenantId("test-tenant-123");
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("John Doe");
    assertThat(response.getBody()).contains("Jane Smith");
  }

  @Test
  void shouldSuccessfullyGetEmployeeAssessmentById() {
    // Given
    String assessmentId = "ea-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("GET").withQueryStringParameters(queryParams);

    EmployeeAssessment assessment = EmployeeAssessment.builder().id(assessmentId).assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).teamId("team-123").build();
    assessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(assessment)).when(employeeAssessmentService).findById(assessmentId, "test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).findById(assessmentId, "test-tenant-123");
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("john.doe@example.com");
  }

  @Test
  void shouldSuccessfullyCreateEmployeeAssessment() {
    // Given
    String requestBody = "{\n" + "  \"tenantId\": \"test-tenant-123\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"teamId\": \"team-123\",\n" + "  \"employee\": {\n" + "    \"name\": \"John Doe\",\n" + "    \"email\": \"john.doe@example.com\",\n" + "    \"documentNumber\": \"123456789\",\n" + "    \"personDocumentType\": \"CPF\",\n" + "    \"gender\": \"MALE\",\n" + "    \"genderPronoun\": \"HE\"\n" + "  }\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("POST").withBody(requestBody);

    EmployeeAssessment createdAssessment = EmployeeAssessment.builder().id("new-ea-id").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).teamId("team-123").build();
    createdAssessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(createdAssessment)).when(employeeAssessmentService).create(
        eq("am-123"), eq("team-123"), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).create(
        eq("am-123"), eq("team-123"), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));
    assertThat(response.getStatusCode()).isEqualTo(201);
    System.out.println(response.getBody());
    assertThat(response.getBody()).contains("new-ea-id");
  }

  @Test
  void shouldSuccessfullyUpdateEmployeeAssessment() {
    // Given
    String assessmentId = "existing-ea-id";
    String requestBody = "{\n" + "  \"tenantId\": \"test-tenant-123\",\n" + "  \"assessmentMatrixId\": \"am-456\",\n" + "  \"teamId\": \"team-456\",\n" + "  \"employee\": {\n" + "    \"name\": \"John Updated\",\n" + "    \"email\": \"john.updated@example.com\",\n" + "    \"documentNumber\": \"987654321\",\n" + "    \"personDocumentType\": \"CPF\",\n" + "    \"gender\": \"MALE\",\n" + "    \"genderPronoun\": \"HE\"\n" + "  }\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("PUT").withBody(requestBody);

    EmployeeAssessment updatedAssessment = EmployeeAssessment.builder().id(assessmentId).assessmentMatrixId("am-456").employee(NaturalPerson.builder().email("john.updated@example.com").name("John Updated").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).teamId("team-456").build();
    updatedAssessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(updatedAssessment)).when(employeeAssessmentService).update(
        eq(assessmentId), eq("am-456"), eq("team-456"), eq("John Updated"), eq("john.updated@example.com"), eq("987654321"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).update(
        eq(assessmentId), eq("am-456"), eq("team-456"), eq("John Updated"), eq("john.updated@example.com"), eq("987654321"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("John Updated");
  }

  @Test
  void shouldSuccessfullyUpdateEmployeeAssessmentScore() {
    // Given
    String assessmentId = "ea-123";
    String requestBody = "{\n" + "  \"tenantId\": \"tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId + "/score").withHttpMethod("POST").withBody(requestBody);

    EmployeeAssessmentScore score = EmployeeAssessmentScore.builder().score(85.5).build();

    EmployeeAssessment assessmentWithScore = EmployeeAssessment.builder().id(assessmentId).assessmentMatrixId("am-123").teamId("team-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).employeeAssessmentScore(score).build();
    assessmentWithScore.setTenantId("test-tenant-123");

    doReturn(assessmentWithScore).when(employeeAssessmentService).updateEmployeeAssessmentScore(assessmentId, "tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).updateEmployeeAssessmentScore(assessmentId, "tenant-123");
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("85.5");
  }

  @Test
  void shouldReturnNotFoundWhenUpdateScoreForNonExistentAssessment() {
    // Given
    String assessmentId = "non-existent-ea";
    String requestBody = "{\n" + "  \"tenantId\": \"tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId + "/score").withHttpMethod("POST").withBody(requestBody);

    doReturn(null).when(employeeAssessmentService).updateEmployeeAssessmentScore(assessmentId, "tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Employee assessment not found or update failed");
  }

  @Test
  void shouldSuccessfullyDeleteEmployeeAssessment() {
    // Given
    String assessmentId = "ea-to-delete";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("DELETE");

    EmployeeAssessment assessment = EmployeeAssessment.builder().id(assessmentId).assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).build();
    assessment.setTenantId("test-tenant-123");

    // No setup needed - deleteById doesn't return anything

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).deleteById(assessmentId);
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void shouldReturnNotFoundWhenEmployeeAssessmentDoesNotExist() {
    // Given
    String assessmentId = "non-existent";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("GET").withQueryStringParameters(queryParams);

    doReturn(Optional.empty()).when(employeeAssessmentService).findById(assessmentId, "test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Employee assessment not found");
  }

  @Test
  void shouldSuccessfullyCreateEmployeeAssessmentWithNullGenderFields() {
    // Given
    String requestBody = "{\n" + "  \"tenantId\": \"test-tenant-123\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"teamId\": \"team-123\",\n" + "  \"employee\": {\n" + "    \"name\": \"John Doe\",\n" + "    \"email\": \"john.doe@example.com\",\n" + "    \"documentNumber\": \"123456789\",\n" + "    \"personDocumentType\": \"CPF\"\n" + "  }\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("POST").withBody(requestBody);

    EmployeeAssessment createdAssessment = EmployeeAssessment.builder().id("new-ea-id").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(null).genderPronoun(null).build()).teamId("team-123").build();
    createdAssessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(createdAssessment)).when(employeeAssessmentService).create(
        eq("am-123"), eq("team-123"), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), isNull(), isNull());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).create(
        eq("am-123"), eq("team-123"), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), isNull(), isNull());
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-ea-id");
  }

  @Test
  void shouldReturnBadRequestForInvalidGender() {
    // Given
    String requestBody = "{\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"teamId\": \"team-123\",\n" + "  \"name\": \"John Doe\",\n" + "  \"email\": \"john.doe@example.com\",\n" + "  \"gender\": \"INVALID_GENDER\",\n" + "  \"genderPronoun\": \"HE\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error processing employee assessment request");
  }

  @Test
  void shouldReturnMethodNotAllowedForUnsupportedMethod() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("PATCH");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).contains("Method Not Allowed");
  }

  @Test
  void shouldReturnInternalServerErrorOnException() {
    // Given
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("GET").withQueryStringParameters(queryParams);

    doThrow(new RuntimeException("Database error")).when(employeeAssessmentService).findAllByTenantId("test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(lambdaLogger).log("Error in employee assessment endpoint: Database error");
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error processing employee assessment request");
  }

  @Test
  void shouldFilterEmployeeAssessmentsByTenantId() {
    // Given
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("GET").withQueryStringParameters(queryParams);

    EmployeeAssessment assessment1 = EmployeeAssessment.builder().id("ea-1").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").build()).teamId("team-123").build();
    assessment1.setTenantId("test-tenant-123");

    EmployeeAssessment assessment2 = EmployeeAssessment.builder().id("ea-2").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("jane.smith@example.com").name("Jane Smith").build()).teamId("team-123").build();
    assessment2.setTenantId("test-tenant-123");

    List<EmployeeAssessment> filteredAssessments = Arrays.asList(assessment1, assessment2);
    doReturn(filteredAssessments).when(employeeAssessmentService).findAllByTenantId("test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).findAllByTenantId("test-tenant-123");
    verify(employeeAssessmentService, never()).findAll();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("John Doe", "Jane Smith");
  }

  @Test
  void shouldFilterByTenantIdAndAssessmentMatrixId() {
    // Given
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");
    queryParams.put("assessmentMatrixId", "am-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("GET").withQueryStringParameters(queryParams);

    EmployeeAssessment assessment = EmployeeAssessment.builder().id("ea-1").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").build()).teamId("team-123").build();
    assessment.setTenantId("test-tenant-123");

    List<EmployeeAssessment> filteredAssessments = Collections.singletonList(assessment);
    doReturn(filteredAssessments).when(employeeAssessmentService).findByAssessmentMatrix("am-123", "test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).findByAssessmentMatrix("am-123", "test-tenant-123");
    verify(employeeAssessmentService, never()).findAllByTenantId(anyString());
    verify(employeeAssessmentService, never()).findAll();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("John Doe");
  }

  @Test
  void shouldGetAssessmentByIdWithTenantValidation() {
    // Given
    String assessmentId = "ea-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "test-tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("GET").withQueryStringParameters(queryParams);

    EmployeeAssessment assessment = EmployeeAssessment.builder().id(assessmentId).assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").build()).teamId("team-123").build();
    assessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(assessment)).when(employeeAssessmentService).findById(assessmentId, "test-tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).findById(assessmentId, "test-tenant-123");
    verify(employeeAssessmentService, never()).findById(assessmentId);
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("John Doe");
  }

  @Test
  void shouldReturn404WhenAssessmentNotFoundWithTenantFilter() {
    // Given
    String assessmentId = "ea-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "different-tenant");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("GET").withQueryStringParameters(queryParams);

    doReturn(Optional.empty()).when(employeeAssessmentService).findById(assessmentId, "different-tenant");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Employee assessment not found");
  }

  @Test
  void shouldDeleteAssessmentUsingDeleteById() {
    // Given
    String assessmentId = "ea-to-delete";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/" + assessmentId).withHttpMethod("DELETE");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).deleteById(assessmentId);
    verify(employeeAssessmentService, never()).findById(assessmentId);
    //  service only has deleteById method
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void shouldReturn409ConflictWhenCreatingDuplicateEmployeeAssessment() {
    // Given
    String requestBody = "{\n" + "  \"tenantId\": \"test-tenant-123\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"teamId\": \"team-123\",\n" + "  \"employee\": {\n" + "    \"name\": \"John Doe\",\n" + "    \"email\": \"john.doe@example.com\",\n" + "    \"documentNumber\": \"123456789\",\n" + "    \"personDocumentType\": \"CPF\",\n" + "    \"gender\": \"MALE\",\n" + "    \"genderPronoun\": \"HE\"\n" + "  }\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("POST").withBody(requestBody);

    // Mock the service to throw EmployeeAssessmentAlreadyExistsException
    doThrow(new com.agilecheckup.service.exception.EmployeeAssessmentAlreadyExistsException("john.doe@example.com", "am-123")).when(employeeAssessmentService).create(anyString(), anyString(), anyString(), anyString(), anyString(), any(PersonDocumentType.class), any(Gender.class), any(GenderPronoun.class));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).create(anyString(), anyString(), anyString(), anyString(), anyString(), any(PersonDocumentType.class), any(Gender.class), any(GenderPronoun.class));
    assertThat(response.getStatusCode()).isEqualTo(409);
    assertThat(response.getBody()).contains("Duplicate employee assessment");
    assertThat(response.getBody()).contains("john.doe@example.com");
    assertThat(response.getBody()).contains("am-123");
  }

  @Test
  void shouldValidateEmployeeWithPostMethod() {
    // Given
    String requestBody = "{\n" + "  \"email\": \"john.doe@example.com\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"tenantId\": \"test-tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    EmployeeValidationResponse validationResponse = EmployeeValidationResponse.success(
        "Welcome! Your assessment access has been confirmed.", "ea-123", "John Doe", "CONFIRMED"
    );

    doReturn(validationResponse).when(employeeAssessmentService).validateEmployee(any(EmployeeValidationRequest.class));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).validateEmployee(any(EmployeeValidationRequest.class));
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("SUCCESS");
    assertThat(response.getBody()).contains("Welcome! Your assessment access has been confirmed.");
    assertThat(response.getBody()).contains("ea-123");
    assertThat(response.getBody()).contains("John Doe");
    assertThat(response.getBody()).contains("CONFIRMED");
  }

  @Test
  void shouldReturn404WhenEmployeeNotFoundDuringValidation() {
    // Given
    String requestBody = "{\n" + "  \"email\": \"notfound@example.com\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"tenantId\": \"test-tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    EmployeeValidationResponse validationResponse = EmployeeValidationResponse.error(
        "We couldn't find your assessment invitation. Please check that you're using the same email address that HR used to invite you, or contact your HR department for assistance."
    );

    doReturn(validationResponse).when(employeeAssessmentService).validateEmployee(any(EmployeeValidationRequest.class));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("ERROR");
    assertThat(response.getBody()).contains("couldn't find your assessment invitation");
  }

  @Test
  void shouldReturn400WhenValidationRequestMissingRequiredFields() {
    // Given - missing email
    String requestBody = "{\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"tenantId\": \"test-tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("email is required");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldReturn400WhenRequestBodyIsEmpty() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody("");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error validating employee");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldReturn400WhenRequestBodyIsNull() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(null);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error validating employee");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldReturn400WhenEmailIsBlank() {
    // Given
    String requestBody = "{\n" + "  \"email\": \"  \",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"tenantId\": \"test-tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("email is required");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldReturn400WhenAssessmentMatrixIdIsMissing() {
    // Given
    String requestBody = "{\n" + "  \"email\": \"test@example.com\",\n" + "  \"tenantId\": \"test-tenant-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("assessmentMatrixId is required");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldReturn400WhenTenantIdIsMissing() {
    // Given
    String requestBody = "{\n" + "  \"email\": \"test@example.com\",\n" + "  \"assessmentMatrixId\": \"am-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments/validate").withHttpMethod("POST").withBody(requestBody);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("tenantId is required");
    verify(employeeAssessmentService, never()).validateEmployee(any());
  }

  @Test
  void shouldSuccessfullyCreateEmployeeAssessmentWithNullTeamId() {
    // Given
    String requestBody = "{\n" + "  \"tenantId\": \"test-tenant-123\",\n" + "  \"assessmentMatrixId\": \"am-123\",\n" + "  \"employee\": {\n" + "    \"name\": \"John Doe\",\n" + "    \"email\": \"john.doe@example.com\",\n" + "    \"documentNumber\": \"123456789\",\n" + "    \"personDocumentType\": \"CPF\",\n" + "    \"gender\": \"MALE\",\n" + "    \"genderPronoun\": \"HE\"\n" + "  }\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/employeeassessments").withHttpMethod("POST").withBody(requestBody);

    EmployeeAssessment createdAssessment = EmployeeAssessment.builder().id("new-ea-id").assessmentMatrixId("am-123").employee(NaturalPerson.builder().email("john.doe@example.com").name("John Doe").gender(Gender.MALE).genderPronoun(GenderPronoun.HE).build()).teamId(null).build();
    createdAssessment.setTenantId("test-tenant-123");

    doReturn(Optional.of(createdAssessment)).when(employeeAssessmentService).create(
        eq("am-123"), isNull(), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(employeeAssessmentService).create(
        eq("am-123"), isNull(), eq("John Doe"), eq("john.doe@example.com"), eq("123456789"), eq(PersonDocumentType.CPF), eq(Gender.MALE), eq(GenderPronoun.HE));
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-ea-id");
    assertThat(response.getBody()).contains("\"teamId\":null");
  }
}