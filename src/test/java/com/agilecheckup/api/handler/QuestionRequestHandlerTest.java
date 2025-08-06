package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.QuestionType;
import com.agilecheckup.persistency.entity.question.AnswerV2;
import com.agilecheckup.persistency.entity.question.QuestionV2;
import com.agilecheckup.service.AssessmentNavigationServiceV2;
import com.agilecheckup.service.QuestionServiceV2;
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
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionRequestHandlerTest {

    @Mock
    private ServiceComponent serviceComponent;

    @Mock
    private QuestionServiceV2 questionService;

    @Mock
    private AssessmentNavigationServiceV2 assessmentNavigationService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger lambdaLogger;

    private QuestionRequestHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        lenient().doReturn(questionService).when(serviceComponent).buildQuestionServiceV2();
        lenient().doReturn(assessmentNavigationService).when(serviceComponent).buildAssessmentNavigationServiceV2();
        lenient().doReturn(lambdaLogger).when(context).getLogger();
        handler = new QuestionRequestHandler(serviceComponent, objectMapper);
    }

    @Test
    void shouldSuccessfullyGetAllQuestions() {
        // Given
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tenantId", "tenant-123");
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        QuestionV2 question1 = QuestionV2.builder()
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

        QuestionV2 question2 = QuestionV2.builder()
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

        List<QuestionV2> questions = Arrays.asList(question1, question2);
        doReturn(questions).when(questionService).findAllByTenantId("tenant-123");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        verify(questionService).findAllByTenantId("tenant-123");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).contains("How effective is your team?");
        assertThat(response.getBody()).contains("Rate your satisfaction");
    }

    @Test
    void shouldReturnBadRequestWhenTenantIdMissingInGetAll() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("GET");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Missing required query parameter: tenantId");
    }

    @Test
    void shouldSuccessfullyGetQuestionById() {
        // Given
        String questionId = "q-123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("GET");

        QuestionV2 question = QuestionV2.builder()
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

        QuestionV2 question1 = QuestionV2.builder()
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

        List<QuestionV2> questions = Arrays.asList(question1);
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
                "  \"categoryId\": \"category-123\",\n" +
                "  \"extraDescription\": \"Test extra description\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions")
                .withHttpMethod("POST")
                .withBody(requestBody);

        QuestionV2 createdQuestion = QuestionV2.builder()
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
                eq("category-123"),
                eq("Test extra description")
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
                eq("category-123"),
                eq("Test extra description")
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
                "  \"categoryId\": \"category-123\",\n" +
                "  \"extraDescription\": \"Test extra description\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/custom")
                .withHttpMethod("POST")
                .withBody(requestBody);

        QuestionV2 createdQuestion = QuestionV2.builder()
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
                eq("category-123"),
                eq("Test extra description")
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
                eq("category-123"),
                eq("Test extra description")
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
                "  \"categoryId\": \"category-123\",\n" +
                "  \"extraDescription\": \"Test extra description\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        QuestionV2 updatedQuestion = QuestionV2.builder()
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
                eq("category-123"),
                eq("Test extra description")
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
                eq("category-123"),
                eq("Test extra description")
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
                "  \"categoryId\": \"category-123\",\n" +
                "  \"extraDescription\": \"Test extra description\"\n" +
                "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/" + questionId + "/custom")
                .withHttpMethod("PUT")
                .withBody(requestBody);

        QuestionV2 updatedQuestion = QuestionV2.builder()
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
                eq("category-123"),
                eq("Test extra description")
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
                eq("category-123"),
                eq("Test extra description")
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

        QuestionV2 question = QuestionV2.builder()
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
                "  \"categoryId\": \"category-123\",\n" +
                "  \"extraDescription\": \"Test extra description\"\n" +
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
    void shouldSuccessfullyGetNextQuestion() {
        // Given
        String employeeAssessmentId = "ea-123";
        String tenantId = "tenant-123";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("employeeAssessmentId", employeeAssessmentId);
        queryParams.put("tenantId", tenantId);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/next")
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        QuestionV2 nextQuestion = QuestionV2.builder()
                .id("q-123")
                .question("What is your experience with agile?")
                .questionType(QuestionType.YES_NO)
                .tenantId(tenantId)
                .assessmentMatrixId("am-123")
                .pillarId("pillar-123")
                .pillarName("Agile Practices")
                .categoryId("category-123")
                .categoryName("Experience")
                .build();

        AnswerV2 existingAnswer = AnswerV2.builder()
                .id("a-123")
                .questionId("q-123")
                .employeeAssessmentId(employeeAssessmentId)
                .value("Yes")
                .tenantId(tenantId)
                .pillarId("pillar-123")
                .categoryId("category-123")
                .questionType(QuestionType.YES_NO)
                .answeredAt(LocalDateTime.now())
                .build();

        AnswerWithProgressResponse response = AnswerWithProgressResponse.builder()
                .question(nextQuestion)
                .existingAnswer(existingAnswer)
                .currentProgress(5)
                .totalQuestions(20)
                .build();

        doReturn(response).when(assessmentNavigationService)
                .getNextUnansweredQuestion(employeeAssessmentId, tenantId);

        // When
        APIGatewayProxyResponseEvent apiResponse = handler.handleRequest(request, context);

        // Then
        verify(assessmentNavigationService).getNextUnansweredQuestion(employeeAssessmentId, tenantId);
        assertThat(apiResponse.getStatusCode()).isEqualTo(200);
        assertThat(apiResponse.getBody()).contains("q-123");
        assertThat(apiResponse.getBody()).contains("What is your experience with agile?");
        assertThat(apiResponse.getBody()).contains("\"currentProgress\":5");
        assertThat(apiResponse.getBody()).contains("\"totalQuestions\":20");
    }

    @Test
    void shouldReturnNotFoundWhenNoMoreQuestions() {
        // Given
        String employeeAssessmentId = "ea-123";
        String tenantId = "tenant-123";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("employeeAssessmentId", employeeAssessmentId);
        queryParams.put("tenantId", tenantId);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/next")
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        AnswerWithProgressResponse response = AnswerWithProgressResponse.builder()
                .question(null) // No more questions
                .existingAnswer(null)
                .currentProgress(20)
                .totalQuestions(20)
                .build();

        doReturn(response).when(assessmentNavigationService)
                .getNextUnansweredQuestion(employeeAssessmentId, tenantId);

        // When
        APIGatewayProxyResponseEvent apiResponse = handler.handleRequest(request, context);

        // Then
        verify(assessmentNavigationService).getNextUnansweredQuestion(employeeAssessmentId, tenantId);
        assertThat(apiResponse.getStatusCode()).isEqualTo(404);
        assertThat(apiResponse.getBody()).contains("\"question\":null");
        assertThat(apiResponse.getBody()).contains("\"currentProgress\":20");
        assertThat(apiResponse.getBody()).contains("\"totalQuestions\":20");
    }

    @Test
    void shouldReturnBadRequestWhenMissingRequiredParametersForNextQuestion() {
        // Given
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tenantId", "tenant-123"); // Missing employeeAssessmentId

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/next")
                .withHttpMethod("GET")
                .withQueryStringParameters(queryParams);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Missing required query parameters: employeeAssessmentId, tenantId");
    }

    @Test
    void shouldReturnBadRequestWhenMissingAllParametersForNextQuestion() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/questions/next")
                .withHttpMethod("GET");
        // No query parameters

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("Missing required query parameters: employeeAssessmentId, tenantId");
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