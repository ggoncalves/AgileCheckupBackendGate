package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.QuestionType;
import com.agilecheckup.persistency.entity.question.Question;
import com.agilecheckup.persistency.entity.question.QuestionOption;
import com.agilecheckup.service.QuestionService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionRequestHandlerTest {

    @Mock
    private ServiceComponent serviceComponent;

    @Mock
    private QuestionService questionService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private QuestionRequestHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        lenient().doReturn(questionService).when(serviceComponent).buildQuestionService();
        lenient().doReturn(lambdaLogger).when(context).getLogger();
        handler = new QuestionRequestHandler(serviceComponent, objectMapper);
    }

    @Test
    void shouldSuccessfullyGetAllQuestions() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("GET");

        Question question1 = Question.builder()
                .id("q-1")
                .question("How effective is your team?")
                .questionType(QuestionType.YES_NO)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        Question question2 = Question.builder()
                .id("q-2")
                .question("Rate your satisfaction")
                .questionType(QuestionType.ONE_TO_TEN)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        PaginatedScanList<Question> questions = mock(PaginatedScanList.class);
        doReturn(Arrays.asList(question1, question2).iterator()).when(questions).iterator();
        doReturn(questions).when(questionService).findAll();

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).findAll();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("How effective is your team?");
        assertThat(response.getBody()).contains("Rate your satisfaction");
    }

    @Test
    void shouldSuccessfullyGetQuestionById() {
        // Given
        String questionId = "q-123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("GET");

        Question question = Question.builder()
                .id(questionId)
                .question("How effective is your team?")
                .questionType(QuestionType.YES_NO)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(question)).when(questionService).findById(questionId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).findById(questionId);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("How effective is your team?");
    }

    @Test
    void shouldSuccessfullyGetQuestionsByAssessmentMatrixId() {
        // Given
        String matrixId = "matrix-123";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tenantId", "tenant-123");
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/matrix/" + matrixId)
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        Question question1 = Question.builder()
                .id("q-1")
                .question("Team effectiveness question")
                .assessmentMatrixId(matrixId)
                .questionType(QuestionType.YES_NO)
                .tenantId("tenant-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        List<Question> questions = Arrays.asList(question1);
        doReturn(questions).when(questionService).findByAssessmentMatrixId(eq(matrixId), eq("tenant-123"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).findByAssessmentMatrixId(eq(matrixId), eq("tenant-123"));
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("Team effectiveness question");
    }

    @Test
    void shouldSuccessfullyCreateQuestion() {
        // Given
        String requestBody = "{\n" +
                "  \"question\": \"How effective is your team?\",\n" +
                "  \"questionType\": \"YES_NO\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"points\": 10.0,\n" +
                "  \"assessmentMatrixId\": \"matrix-123\",\n" +
                "  \"pillarId\": \"pillar-123\",\n" +
                "  \"categoryId\": \"category-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("POST")
                .withBody(requestBody);

        Question createdQuestion = Question.builder()
                .id("new-question-id")
                .question("How effective is your team?")
                .questionType(QuestionType.YES_NO)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(createdQuestion)).when(questionService).create(
                eq("How effective is your team?"),
                eq(QuestionType.YES_NO),
                eq("tenant-123"),
                eq(10.0),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).create(
                eq("How effective is your team?"),
                eq(QuestionType.YES_NO),
                eq("tenant-123"),
                eq(10.0),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("new-question-id");
    }

    @Test
    void shouldSuccessfullyCreateCustomQuestion() {
        // Given
        String requestBody = "{\n" +
                "  \"question\": \"Custom question with options\",\n" +
                "  \"questionType\": \"CUSTOMIZED\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"isMultipleChoice\": false,\n" +
                "  \"showFlushed\": false,\n" +
                "  \"options\": [\n" +
                "    {\"id\": 1, \"text\": \"Option A\"},\n" +
                "    {\"id\": 2, \"text\": \"Option B\"},\n" +
                "    {\"id\": 3, \"text\": \"Option C\"}\n" +
                "  ],\n" +
                "  \"assessmentMatrixId\": \"matrix-123\",\n" +
                "  \"pillarId\": \"pillar-123\",\n" +
                "  \"categoryId\": \"category-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/custom")
                .withHttpMethod("POST")
                .withBody(requestBody);

        Question createdQuestion = Question.builder()
                .id("new-custom-question-id")
                .question("Custom question with options")
                .questionType(QuestionType.CUSTOMIZED)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(createdQuestion)).when(questionService).createCustomQuestion(
                eq("Custom question with options"),
                eq(QuestionType.CUSTOMIZED),
                eq("tenant-123"),
                eq(false),
                eq(false),
                anyList(),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).createCustomQuestion(
                eq("Custom question with options"),
                eq(QuestionType.CUSTOMIZED),
                eq("tenant-123"),
                eq(false),
                eq(false),
                argThat(list -> list.size() == 3),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).contains("new-custom-question-id");
    }

    @Test
    void shouldSuccessfullyUpdateQuestion() {
        // Given
        String questionId = "existing-question-id";
        String requestBody = "{\n" +
                "  \"question\": \"Updated question text\",\n" +
                "  \"questionType\": \"ONE_TO_TEN\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"points\": 10.0,\n" +
                "  \"assessmentMatrixId\": \"matrix-123\",\n" +
                "  \"pillarId\": \"pillar-123\",\n" +
                "  \"categoryId\": \"category-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        Question updatedQuestion = Question.builder()
                .id(questionId)
                .question("Updated question text")
                .questionType(QuestionType.ONE_TO_TEN)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(updatedQuestion)).when(questionService).update(
                eq(questionId),
                eq("Updated question text"),
                eq(QuestionType.ONE_TO_TEN),
                eq("tenant-123"),
                eq(10.0),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).update(
                eq(questionId),
                eq("Updated question text"),
                eq(QuestionType.ONE_TO_TEN),
                eq("tenant-123"),
                eq(10.0),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("Updated question text");
    }

    @Test
    void shouldSuccessfullyUpdateCustomQuestion() {
        // Given
        String questionId = "existing-custom-question-id";
        String requestBody = "{\n" +
                "  \"question\": \"Updated custom question\",\n" +
                "  \"questionType\": \"CUSTOMIZED\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"isMultipleChoice\": true,\n" +
                "  \"showFlushed\": false,\n" +
                "  \"options\": [\n" +
                "    {\"id\": 1, \"text\": \"New Option 1\"},\n" +
                "    {\"id\": 2, \"text\": \"New Option 2\"}\n" +
                "  ],\n" +
                "  \"assessmentMatrixId\": \"matrix-123\",\n" +
                "  \"pillarId\": \"pillar-123\",\n" +
                "  \"categoryId\": \"category-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId + "/custom")
                .withHttpMethod("PUT")
                .withBody(requestBody);

        Question updatedQuestion = Question.builder()
                .id(questionId)
                .question("Updated custom question")
                .questionType(QuestionType.CUSTOMIZED)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(updatedQuestion)).when(questionService).updateCustomQuestion(
                eq(questionId),
                eq("Updated custom question"),
                eq(QuestionType.CUSTOMIZED),
                eq("tenant-123"),
                eq(true),
                eq(false),
                anyList(),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).updateCustomQuestion(
                eq(questionId),
                eq("Updated custom question"),
                eq(QuestionType.CUSTOMIZED),
                eq("tenant-123"),
                eq(true),
                eq(false),
                argThat(list -> list.size() == 2),
                eq("matrix-123"),
                eq("pillar-123"),
                eq("category-123")
        );
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("Updated custom question");
    }

    @Test
    void shouldSuccessfullyDeleteQuestion() {
        // Given
        String questionId = "question-to-delete";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("DELETE");

        Question question = Question.builder()
                .id(questionId)
                .question("Question to delete")
                .questionType(QuestionType.YES_NO)
                .tenantId("tenant-123")
                .assessmentMatrixId("matrix-123")
                .pillarId("pillar-123")
                .pillarName("Pillar Name")
                .categoryId("category-123")
                .categoryName("Category Name")
                .build();

        doReturn(Optional.of(question)).when(questionService).findById(questionId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).findById(questionId);
        verify(questionService).delete(question);
        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenQuestionDoesNotExist() {
        // Given
        String questionId = "non-existent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("GET");

        doReturn(Optional.empty()).when(questionService).findById(questionId);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getBody()).contains("Question not found");
    }

    @Test
    void shouldReturnBadRequestForInvalidQuestionType() {
        // Given
        String requestBody = "{\n" +
                "  \"question\": \"Invalid type question\",\n" +
                "  \"questionType\": \"INVALID_TYPE\",\n" +
                "  \"tenantId\": \"tenant-123\",\n" +
                "  \"points\": 10.0,\n" +
                "  \"assessmentMatrixId\": \"matrix-123\",\n" +
                "  \"pillarId\": \"pillar-123\",\n" +
                "  \"categoryId\": \"category-123\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("POST")
                .withBody(requestBody);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).contains("Error processing question request");
    }

    @Test
    void shouldReturnMethodNotAllowedForUnsupportedMethod() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("PATCH");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(405);
        assertThat(response.getBody()).contains("Method Not Allowed");
    }
}