package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.AssessmentMatrix;
import com.agilecheckup.persistency.entity.Category;
import com.agilecheckup.persistency.entity.Pillar;
import com.agilecheckup.service.AssessmentMatrixService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentMatrixRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private AssessmentMatrixService assessmentMatrixService;

  @Mock
  private Context context;

  @Captor
  ArgumentCaptor<Map<String, Pillar>> pillarMapCaptor;


  private AssessmentMatrixRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    when(serviceComponent.buildAssessmentMatrixService()).thenReturn(assessmentMatrixService);
    handler = new AssessmentMatrixRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void shouldSuccessfullyCreateAssessmentMatrixWithManualMapBuilding() {
    // Given
    String requestBody = "{\n" +
        "  \"name\": \"Engineering Matrix\",\n" +
        "  \"description\": \"Competency assessment for engineering roles\",\n" +
        "  \"tenantId\": \"tenant-test-id-123\",\n" +
        "  \"performanceCycleId\": \"3837551b-20f2-41eb-9779-8203a5209c45\",\n" +
        "  \"pillarMap\": {\n" +
        "    \"p1\": {\n" +
        "      \"name\": \"Technical Skills\",\n" +
        "      \"description\": \"Technical knowledge and abilities\",\n" +
        "      \"categoryMap\": {\n" +
        "        \"c1\": {\n" +
        "          \"name\": \"Programming\",\n" +
        "          \"description\": \"Programming skills and knowledge\"\n" +
        "        },\n" +
        "        \"c2\": {\n" +
        "          \"name\": \"Architecture\",\n" +
        "          \"description\": \"System design and architecture\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("POST")
        .withBody(requestBody);

    // Create a sample assessment matrix to return
    AssessmentMatrix createdMatrix = AssessmentMatrix.builder()
        .id("new-matrix-id")
        .name("Engineering Matrix")
        .description("Competency assessment for engineering roles")
        .tenantId("tenant-test-id-123")
        .performanceCycleId("3837551b-20f2-41eb-9779-8203a5209c45")
        .build();

    // Set up the service to return the assessment matrix, capture arguments
    when(assessmentMatrixService.create(
        anyString(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(Optional.of(createdMatrix));

    // When - This will be using our new implementation with manual map building
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(assessmentMatrixService).create(
        eq("Engineering Matrix"),
        eq("Competency assessment for engineering roles"),
        eq("tenant-test-id-123"),
        eq("3837551b-20f2-41eb-9779-8203a5209c45"),
        pillarMapCaptor.capture());

    // Verify the pillar map was built correctly
    Map<String, Pillar> capturedPillarMap = pillarMapCaptor.getValue();
    assertThat(capturedPillarMap).containsKey("p1");

    Pillar pillar = capturedPillarMap.get("p1");
    assertThat(pillar.getName()).isEqualTo("Technical Skills");
    assertThat(pillar.getDescription()).isEqualTo("Technical knowledge and abilities");
    assertThat(pillar.getCategoryMap()).containsKeys("c1", "c2");

    Category category1 = pillar.getCategoryMap().get("c1");
    assertThat(category1.getName()).isEqualTo("Programming");
    assertThat(category1.getDescription()).isEqualTo("Programming skills and knowledge");

    // Verify the response
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-matrix-id");
  }

  @Test
  void handleGetAll_whenTenantIdProvided_shouldReturnMatricesForTenant() {
    // Given
    String tenantId = "tenant-123";
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", tenantId);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("GET")
        .withQueryStringParameters(queryParams);

    AssessmentMatrix matrix1 = AssessmentMatrix.builder()
        .id("matrix-1")
        .name("Engineering Matrix")
        .description("For engineering assessments")
        .tenantId(tenantId)
        .performanceCycleId("cycle-1")
        .build();

    AssessmentMatrix matrix2 = AssessmentMatrix.builder()
        .id("matrix-2")
        .name("Sales Matrix")
        .description("For sales assessments")
        .tenantId(tenantId)
        .performanceCycleId("cycle-2")
        .build();

    List<AssessmentMatrix> matrices = Arrays.asList(matrix1, matrix2);

    when(assessmentMatrixService.findAllByTenantId(tenantId)).thenReturn(matrices);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("matrix-1", "Engineering Matrix");
    assertThat(response.getBody()).contains("matrix-2", "Sales Matrix");
    verify(assessmentMatrixService).findAllByTenantId(tenantId);
    verify(assessmentMatrixService, never()).findAll();
  }

  @Test
  void handleGetAll_whenNoTenantIdProvided_shouldReturnBadRequest() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPath("/assessmentmatrices")
        .withHttpMethod("GET");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).isEqualTo("tenantId is required");
    verify(assessmentMatrixService, never()).findAll();
    verify(assessmentMatrixService, never()).findAllByTenantId(anyString());
  }
}