package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.question.Answer;
import com.agilecheckup.service.AnswerService;
import com.agilecheckup.service.AssessmentNavigationService;
import com.agilecheckup.service.dto.AnswerWithProgressResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerRequestHandlerTest {

    @Mock
    private ServiceComponent serviceComponent;

    @Mock
    private AnswerService answerService;

    @Mock
    private AssessmentNavigationService assessmentNavigationService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private AnswerRequestHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        lenient().doReturn(answerService).when(serviceComponent).buildAnswerService();
        lenient().doReturn(assessmentNavigationService).when(serviceComponent).buildAssessmentNavigationService();
        lenient().doReturn(lambdaLogger).when(context).getLogger();
        handler = new AnswerRequestHandler(serviceComponent, objectMapper);
    }

    @Test
    void shouldSuccessfullyGetAllAnswers() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers")
                .withHttpMethod("GET");

        Answer answer1 = new Answer();
        answer1.setId("answer-1");
        answer1.setQuestionId("q-1");
        answer1.setEmployeeAssessmentId("ea-1");
        answer1.setValue("Yes");
        answer1.setAnsweredAt(LocalDateTime.now());
        answer1.setTenantId("tenant-123");

        Answer answer2 = new Answer();
        answer2.setId("answer-2");
        answer2.setQuestionId("q-2");
        answer2.setEmployeeAssessmentId("ea-1");
        answer2.setValue("8");
        answer2.setAnsweredAt(LocalDateTime.now());
        answer2.setTenantId("tenant-123");

        List<Answer> answers = Arrays.asList(answer1, answer2);
        when(answerService.findAll()).thenReturn(answers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).findAll();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("answer-1");
        assertThat(response.getBody()).contains("answer-2");
    }

    @Test
    void shouldSuccessfullyGetAnswerById() {
        // Given
        String answerId = "answer-123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/" + answerId)
                .withHttpMethod("GET");

        Answer answer = new Answer();
        answer.setId(answerId);
        answer.setQuestionId("q-1");
        answer.setEmployeeAssessmentId("ea-1");
        answer.setValue("Yes");
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setTenantId("tenant-123");

        when(answerService.findById(answerId)).thenReturn(Optional.of(answer));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).findById(answerId);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains(answerId);
    }

    @Test
    void shouldSuccessfullyGetAnswersByEmployeeAssessmentId() {
        // Given
        String employeeAssessmentId = "ea-123";
        String tenantId = "tenant-123";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tenantId", tenantId);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/employeeassessment/" + employeeAssessmentId)
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        Answer answer1 = new Answer();
        answer1.setId("answer-1");
        answer1.setQuestionId("q-1");
        answer1.setEmployeeAssessmentId(employeeAssessmentId);
        answer1.setValue("Yes");
        answer1.setAnsweredAt(LocalDateTime.now());
        answer1.setTenantId(tenantId);

        Answer answer2 = new Answer();
        answer2.setId("answer-2");
        answer2.setQuestionId("q-2");
        answer2.setEmployeeAssessmentId(employeeAssessmentId);
        answer2.setValue("8");
        answer2.setAnsweredAt(LocalDateTime.now());
        answer2.setTenantId(tenantId);

        List<Answer> answers = Arrays.asList(answer1, answer2);
        when(answerService.findByEmployeeAssessmentId(employeeAssessmentId, tenantId)).thenReturn(answers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).findByEmployeeAssessmentId(employeeAssessmentId, tenantId);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("answer-1");
        assertThat(response.getBody()).contains("answer-2");
    }

    @Test
    void shouldReturnBadRequestWhenTenantIdMissingForEmployeeAssessment() {
        // Given
        String employeeAssessmentId = "ea-123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/employeeassessment/" + employeeAssessmentId)
                .withHttpMethod("GET");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Missing required query parameter: tenantId");
    }

    @Test
    void shouldSuccessfullyCreateAnswer() {
        // Given
        String requestBody = "{\n" +
                "  \"employeeAssessmentId\": \"ea-123\",\n" +
                "  \"questionId\": \"q-123\",\n" +
                "  \"answeredAt\": \"2024-01-01T10:00:00\",\n" +
                "  \"value\": \"Yes\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"notes\": null\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers")
                .withHttpMethod("POST")
                .withBody(requestBody);

        Answer createdAnswer = new Answer();
        createdAnswer.setId("new-answer-id");
        createdAnswer.setQuestionId("q-123");
        createdAnswer.setEmployeeAssessmentId("ea-123");
        createdAnswer.setValue("Yes");
        createdAnswer.setAnsweredAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        createdAnswer.setTenantId("tenant-123");

        when(answerService.create(
                eq("ea-123"),
                eq("q-123"),
                any(LocalDateTime.class),
                eq("Yes"),
                eq("tenant-123"),
                isNull()
        )).thenReturn(Optional.of(createdAnswer));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).create(
                eq("ea-123"),
                eq("q-123"),
                any(LocalDateTime.class),
                eq("Yes"),
                eq("tenant-123"),
                isNull()
        );
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("new-answer-id");
    }

    @Test
    void shouldSuccessfullyUpdateAnswer() {
        // Given
        String answerId = "existing-answer-id";
        String requestBody = "{\n" +
                "  \"answeredAt\": \"2024-01-01T11:00:00\",\n" +
                "  \"value\": \"No\",\n" +
                "  \"notes\": \"Updated answer\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/" + answerId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        Answer updatedAnswer = new Answer();
        updatedAnswer.setId(answerId);
        updatedAnswer.setQuestionId("q-123");
        updatedAnswer.setEmployeeAssessmentId("ea-123");
        updatedAnswer.setValue("No");
        updatedAnswer.setAnsweredAt(LocalDateTime.of(2024, 1, 1, 11, 0));
        updatedAnswer.setTenantId("tenant-123");

        when(answerService.update(
                eq(answerId),
                any(LocalDateTime.class),
                eq("No"),
                eq("Updated answer")
        )).thenReturn(Optional.of(updatedAnswer));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).update(
                eq(answerId),
                any(LocalDateTime.class),
                eq("No"),
                eq("Updated answer")
        );
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("No");
    }

    @Test
    void shouldSuccessfullyDeleteAnswer() {
        // Given
        String answerId = "answer-to-delete";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/" + answerId)
                .withHttpMethod("DELETE");

        when(answerService.deleteById(answerId)).thenReturn(true);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).deleteById(answerId);
        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenAnswerDoesNotExist() {
        // Given
        String answerId = "non-existent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/" + answerId)
                .withHttpMethod("GET");

        when(answerService.findById(answerId)).thenReturn(Optional.empty());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("Answer not found");
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentAnswer() {
        // Given
        String answerId = "non-existent-id";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/" + answerId)
                .withHttpMethod("DELETE");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(answerService).deleteById(answerId);
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("Answer not found");
    }

    @Test
    void shouldReturnBadRequestWhenCreateFails() {
        // Given
        String requestBody = "{\n" +
                "  \"employeeAssessmentId\": \"ea-123\",\n" +
                "  \"questionId\": \"q-123\",\n" +
                "  \"answeredAt\": \"2024-01-01T10:00:00\",\n" +
                "  \"value\": \"Yes\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"notes\": null\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers")
                .withHttpMethod("POST")
                .withBody(requestBody);

        doReturn(Optional.empty())
            .when(answerService).create(
                anyString(), anyString(), any(LocalDateTime.class),
                anyString(), anyString(), nullable(String.class));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Failed to create answer");
    }

    @Test
    void shouldReturnMethodNotAllowedForUnsupportedMethod() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers")
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
                .withPath("/answers")
                .withHttpMethod("GET");

        when(answerService.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(lambdaLogger).log("Error in answer endpoint: Database error");
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Error processing answer request");
    }

    @Test
    void shouldSuccessfullySaveAnswerAndGetNext() throws Exception {
        // Given
        String employeeAssessmentId = "ea123";
        String questionId = "q1";
        String tenantId = "tenant123";
        String value = "Yes";
        String notes = "Test notes";
        LocalDateTime answeredAt = LocalDateTime.of(2024, 1, 1, 10, 0);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("employeeAssessmentId", employeeAssessmentId);
        requestBody.put("questionId", questionId);
        requestBody.put("answeredAt", answeredAt.toString());
        requestBody.put("value", value);
        requestBody.put("tenantId", tenantId);
        requestBody.put("notes", notes);

        AnswerWithProgressResponse mockResponse = AnswerWithProgressResponse.builder()
                .question(null) // Null indicates assessment completed
                .existingAnswer(null)
                .currentProgress(5)
                .totalQuestions(5)
                .build();

        when(assessmentNavigationService.saveAnswerAndGetNext(
                eq(employeeAssessmentId), eq(questionId), eq(answeredAt), 
                eq(value), eq(tenantId), eq(notes)
        )).thenReturn(mockResponse);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/save-and-next")
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(requestBody));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(200);
        
        AnswerWithProgressResponse responseBody = objectMapper.readValue(
                response.getBody(), AnswerWithProgressResponse.class);
        assertThat(responseBody.getCurrentProgress()).isEqualTo(5);
        assertThat(responseBody.getTotalQuestions()).isEqualTo(5);
        assertThat(responseBody.getQuestion()).isNull();

        verify(assessmentNavigationService).saveAnswerAndGetNext(
                eq(employeeAssessmentId), eq(questionId), eq(answeredAt), 
                eq(value), eq(tenantId), eq(notes)
        );
    }

    @Test
    void shouldReturnBadRequestWhenSaveAnswerAndGetNextFails() throws Exception {
        // Given
        String employeeAssessmentId = "ea123";
        String questionId = "q1";
        String tenantId = "tenant123";
        String value = "Yes";
        String notes = "Test notes";
        LocalDateTime answeredAt = LocalDateTime.of(2024, 1, 1, 10, 0);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("employeeAssessmentId", employeeAssessmentId);
        requestBody.put("questionId", questionId);
        requestBody.put("answeredAt", answeredAt.toString());
        requestBody.put("value", value);
        requestBody.put("tenantId", tenantId);
        requestBody.put("notes", notes);

        when(assessmentNavigationService.saveAnswerAndGetNext(
                eq(employeeAssessmentId), eq(questionId), eq(answeredAt), 
                eq(value), eq(tenantId), eq(notes)
        )).thenThrow(new RuntimeException("Failed to save answer"));

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/save-and-next")
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(requestBody));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Failed to save answer");
    }

    @Test
    void shouldReturnMethodNotAllowedForSaveAndNextWithGetMethod() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/save-and-next")
                .withHttpMethod("GET");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(405);
        assertThat(response.getBody()).contains("Method Not Allowed");
    }

    @Test
    void shouldHandleISO8601DateTimeWithMillisecondsAndTimezone() throws Exception {
        // Given - This test reproduces the bug found in CloudWatch logs
        String employeeAssessmentId = "ea123";
        String questionId = "q1";
        String tenantId = "tenant123";
        String value = "Yes";
        String notes = "Test notes";
        // Frontend sends ISO8601 with milliseconds and timezone
        String answeredAtWithMillisAndTz = "2025-06-19T17:54:27.862Z";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("employeeAssessmentId", employeeAssessmentId);
        requestBody.put("questionId", questionId);
        requestBody.put("answeredAt", answeredAtWithMillisAndTz);
        requestBody.put("value", value);
        requestBody.put("tenantId", tenantId);
        requestBody.put("notes", notes);

        AnswerWithProgressResponse mockResponse = AnswerWithProgressResponse.builder()
                .question(null)
                .existingAnswer(null)
                .currentProgress(1)
                .totalQuestions(1)
                .build();

        when(assessmentNavigationService.saveAnswerAndGetNext(
                eq(employeeAssessmentId), eq(questionId), any(LocalDateTime.class), 
                eq(value), eq(tenantId), eq(notes)
        )).thenReturn(mockResponse);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/answers/save-and-next")
                .withHttpMethod("POST")
                .withBody(objectMapper.writeValueAsString(requestBody));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(200);
        
        AnswerWithProgressResponse responseBody = objectMapper.readValue(
                response.getBody(), AnswerWithProgressResponse.class);
        assertThat(responseBody.getCurrentProgress()).isEqualTo(1);
        assertThat(responseBody.getTotalQuestions()).isEqualTo(1);

        verify(assessmentNavigationService).saveAnswerAndGetNext(
                eq(employeeAssessmentId), eq(questionId), any(LocalDateTime.class), 
                eq(value), eq(tenantId), eq(notes)
        );
    }
}