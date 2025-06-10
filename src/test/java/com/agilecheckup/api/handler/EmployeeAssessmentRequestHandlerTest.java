package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.EmployeeAssessment;
import com.agilecheckup.persistency.entity.EmployeeAssessmentScore;
import com.agilecheckup.persistency.entity.person.NaturalPerson;
import com.agilecheckup.persistency.entity.person.Gender;
import com.agilecheckup.persistency.entity.person.GenderPronoun;
import com.agilecheckup.persistency.entity.person.PersonDocumentType;
import com.agilecheckup.service.EmployeeAssessmentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("GET");

        EmployeeAssessment assessment1 = EmployeeAssessment.builder()
                .id("ea-1")
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .teamId("team-123")
                .build();

        EmployeeAssessment assessment2 = EmployeeAssessment.builder()
                .id("ea-2")
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("jane.smith@example.com")
                        .name("Jane Smith")
                        .gender(Gender.FEMALE)
                        .genderPronoun(GenderPronoun.SHE)
                        .build())
                .teamId("team-123")
                .build();

        PaginatedScanList<EmployeeAssessment> assessments = mock(PaginatedScanList.class);
        doReturn(Arrays.asList(assessment1, assessment2).iterator()).when(assessments).iterator();
        doReturn(assessments).when(employeeAssessmentService).findAll();

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).findAll();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("John Doe");
        assertThat(response.getBody()).contains("Jane Smith");
    }

    @Test
    void shouldSuccessfullyGetEmployeeAssessmentById() {
        // Given
        String assessmentId = "ea-123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId)
                .withHttpMethod("GET");

        EmployeeAssessment assessment = EmployeeAssessment.builder()
                .id(assessmentId)
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .teamId("team-123")
                .build();

        doReturn(Optional.of(assessment)).when(employeeAssessmentService).findById(assessmentId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).findById(assessmentId);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("john.doe@example.com");
    }

    @Test
    void shouldSuccessfullyCreateEmployeeAssessment() {
        // Given
        String requestBody = "{\n" +
                "  \"assessmentMatrixId\": \"am-123\",\n" +
                "  \"teamId\": \"team-123\",\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"email\": \"john.doe@example.com\",\n" +
                "  \"documentNumber\": \"123456789\",\n" +
                "  \"documentType\": \"CPF\",\n" +
                "  \"gender\": \"MALE\",\n" +
                "  \"genderPronoun\": \"HE\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("POST")
                .withBody(requestBody);

        EmployeeAssessment createdAssessment = EmployeeAssessment.builder()
                .id("new-ea-id")
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .teamId("team-123")
                .build();

        doReturn(Optional.of(createdAssessment)).when(employeeAssessmentService).create(
                eq("am-123"),
                eq("team-123"),
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("123456789"),
                eq(PersonDocumentType.CPF),
                eq(Gender.MALE),
                eq(GenderPronoun.HE)
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).create(
                eq("am-123"),
                eq("team-123"),
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("123456789"),
                eq(PersonDocumentType.CPF),
                eq(Gender.MALE),
                eq(GenderPronoun.HE)
        );
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("new-ea-id");
    }

    @Test
    void shouldSuccessfullyUpdateEmployeeAssessment() {
        // Given
        String assessmentId = "existing-ea-id";
        String requestBody = "{\n" +
                "  \"assessmentMatrixId\": \"am-456\",\n" +
                "  \"teamId\": \"team-456\",\n" +
                "  \"name\": \"John Updated\",\n" +
                "  \"email\": \"john.updated@example.com\",\n" +
                "  \"documentNumber\": \"987654321\",\n" +
                "  \"documentType\": \"CPF\",\n" +
                "  \"gender\": \"MALE\",\n" +
                "  \"genderPronoun\": \"HE\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        EmployeeAssessment updatedAssessment = EmployeeAssessment.builder()
                .id(assessmentId)
                .assessmentMatrixId("am-456")
                .employee(NaturalPerson.builder()
                        .email("john.updated@example.com")
                        .name("John Updated")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .teamId("team-456")
                .build();

        doReturn(Optional.of(updatedAssessment)).when(employeeAssessmentService).update(
                eq(assessmentId),
                eq("am-456"),
                eq("team-456"),
                eq("John Updated"),
                eq("john.updated@example.com"),
                eq("987654321"),
                eq(PersonDocumentType.CPF),
                eq(Gender.MALE),
                eq(GenderPronoun.HE)
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).update(
                eq(assessmentId),
                eq("am-456"),
                eq("team-456"),
                eq("John Updated"),
                eq("john.updated@example.com"),
                eq("987654321"),
                eq(PersonDocumentType.CPF),
                eq(Gender.MALE),
                eq(GenderPronoun.HE)
        );
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("John Updated");
    }

    @Test
    void shouldSuccessfullyUpdateEmployeeAssessmentScore() {
        // Given
        String assessmentId = "ea-123";
        String requestBody = "{\n" +
                "  \"tenantId\": \"tenant-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId + "/score")
                .withHttpMethod("POST")
                .withBody(requestBody);

        EmployeeAssessmentScore score = EmployeeAssessmentScore.builder()
                .score(85.5)
                .build();

        EmployeeAssessment assessmentWithScore = EmployeeAssessment.builder()
                .id(assessmentId)
                .assessmentMatrixId("am-123")
                .teamId("team-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .employeeAssessmentScore(score)
                .build();
        
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
        String requestBody = "{\n" +
                "  \"tenantId\": \"tenant-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId + "/score")
                .withHttpMethod("POST")
                .withBody(requestBody);

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
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId)
                .withHttpMethod("DELETE");

        EmployeeAssessment assessment = EmployeeAssessment.builder()
                .id(assessmentId)
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(Gender.MALE)
                        .genderPronoun(GenderPronoun.HE)
                        .build())
                .build();

        doReturn(Optional.of(assessment)).when(employeeAssessmentService).findById(assessmentId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).findById(assessmentId);
        verify(employeeAssessmentService).delete(assessment);
        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenEmployeeAssessmentDoesNotExist() {
        // Given
        String assessmentId = "non-existent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments/" + assessmentId)
                .withHttpMethod("GET");

        doReturn(Optional.empty()).when(employeeAssessmentService).findById(assessmentId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("Employee assessment not found");
    }

    @Test
    void shouldSuccessfullyCreateEmployeeAssessmentWithNullGenderFields() {
        // Given
        String requestBody = "{\n" +
                "  \"assessmentMatrixId\": \"am-123\",\n" +
                "  \"teamId\": \"team-123\",\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"email\": \"john.doe@example.com\",\n" +
                "  \"documentNumber\": \"123456789\",\n" +
                "  \"documentType\": \"CPF\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("POST")
                .withBody(requestBody);

        EmployeeAssessment createdAssessment = EmployeeAssessment.builder()
                .id("new-ea-id")
                .assessmentMatrixId("am-123")
                .employee(NaturalPerson.builder()
                        .email("john.doe@example.com")
                        .name("John Doe")
                        .gender(null)
                        .genderPronoun(null)
                        .build())
                .teamId("team-123")
                .build();

        doReturn(Optional.of(createdAssessment)).when(employeeAssessmentService).create(
                eq("am-123"),
                eq("team-123"),
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("123456789"),
                eq(PersonDocumentType.CPF),
                eq(null),
                eq(null)
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(employeeAssessmentService).create(
                eq("am-123"),
                eq("team-123"),
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("123456789"),
                eq(PersonDocumentType.CPF),
                eq(null),
                eq(null)
        );
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("new-ea-id");
    }

    @Test
    void shouldReturnBadRequestForInvalidGender() {
        // Given
        String requestBody = "{\n" +
                "  \"assessmentMatrixId\": \"am-123\",\n" +
                "  \"teamId\": \"team-123\",\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"email\": \"john.doe@example.com\",\n" +
                "  \"gender\": \"INVALID_GENDER\",\n" +
                "  \"genderPronoun\": \"HE\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("POST")
                .withBody(requestBody);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Error processing employee assessment request");
    }

    @Test
    void shouldReturnMethodNotAllowedForUnsupportedMethod() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("PATCH");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(405);
        assertThat(response.getBody()).contains("Method Not Allowed");
    }

    @Test
    void shouldReturnInternalServerErrorOnException() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/employeeassessments")
                .withHttpMethod("GET");

        doThrow(new RuntimeException("Database error")).when(employeeAssessmentService).findAll();

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(lambdaLogger).log("Error in employee assessment endpoint: Database error");
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Error processing employee assessment request");
    }
}