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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

  private ObjectMapper objectMapper;
  private AssessmentMatrixRequestHandler handler;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
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
        anyString(), anyString(), anyString(), anyString(), any(Map.class)))
        .thenReturn(Optional.of(createdMatrix));

    // Create an argumnet captor to verify the pillarMap is built correctly
    ArgumentCaptor<Map<String, Pillar>> pillarMapCaptor = ArgumentCaptor.forClass(Map.class);

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
}