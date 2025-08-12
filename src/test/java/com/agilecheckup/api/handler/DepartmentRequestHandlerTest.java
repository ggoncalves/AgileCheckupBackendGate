package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.Department;
import com.agilecheckup.service.DepartmentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DepartmentRequestHandlerTest {

  @Mock
  private ServiceComponent serviceComponent;

  @Mock
  private DepartmentService departmentService;

  @Mock
  private Context context;

  @Mock
  private LambdaLogger lambdaLogger;

  private DepartmentRequestHandler handler;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    lenient().doReturn(departmentService).when(serviceComponent).buildDepartmentService();
    lenient().doReturn(lambdaLogger).when(context).getLogger();
    handler = new DepartmentRequestHandler(serviceComponent, objectMapper);
  }

  @Test
  void shouldSuccessfullyGetAllDepartments() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("GET");

    Department dept1 = new Department();
    dept1.setId("dept-1");
    dept1.setName("Engineering");
    dept1.setDescription("Engineering department");
    dept1.setTenantId("tenant-123");
    dept1.setCompanyId("company-123");

    Department dept2 = new Department();
    dept2.setId("dept-2");
    dept2.setName("Sales");
    dept2.setDescription("Sales department");
    dept2.setTenantId("tenant-123");
    dept2.setCompanyId("company-123");

    doReturn(Arrays.asList(dept1, dept2)).when(departmentService).findAll();

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findAll();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("Engineering");
    assertThat(response.getBody()).contains("Sales");
  }

  @Test
  void shouldSuccessfullyGetDepartmentsByTenantId() {
    // Given
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("tenantId", "tenant-123");

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("GET").withQueryStringParameters(queryParams);

    Department dept1 = new Department();
    dept1.setId("dept-1");
    dept1.setName("Engineering");
    dept1.setDescription("Engineering department");
    dept1.setTenantId("tenant-123");
    dept1.setCompanyId("company-123");

    doReturn(Arrays.asList(dept1)).when(departmentService).findAllByTenantId("tenant-123");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findAllByTenantId("tenant-123");
    verify(departmentService, never()).findAll();
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("Engineering");
  }

  @Test
  void shouldSuccessfullyGetDepartmentById() {
    // Given
    String departmentId = "dept-123";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("GET");

    Department department = new Department();
    department.setId(departmentId);
    department.setName("Engineering");
    department.setDescription("Engineering department");
    department.setTenantId("tenant-123");
    department.setCompanyId("company-123");

    doReturn(Optional.of(department)).when(departmentService).findById(departmentId);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findById(departmentId);
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("Engineering");
    assertThat(response.getBody()).contains(departmentId);
  }

  @Test
  void shouldReturnNotFoundWhenDepartmentDoesNotExist() {
    // Given
    String departmentId = "non-existent";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("GET");

    doReturn(Optional.empty()).when(departmentService).findById(departmentId);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findById(departmentId);
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Department not found");
  }

  @Test
  void shouldSuccessfullyCreateDepartment() {
    // Given
    String requestBody = "{\n" + "  \"name\": \"Engineering\",\n" + "  \"description\": \"Engineering department\",\n" + "  \"tenantId\": \"tenant-123\",\n" + "  \"companyId\": \"company-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("POST").withBody(requestBody);

    Department createdDepartment = new Department();
    createdDepartment.setId("new-dept-id");
    createdDepartment.setName("Engineering");
    createdDepartment.setDescription("Engineering department");
    createdDepartment.setTenantId("tenant-123");
    createdDepartment.setCompanyId("company-123");

    doReturn(Optional.of(createdDepartment)).when(departmentService).create(
        eq("Engineering"), eq("Engineering department"), eq("tenant-123"), eq("company-123")
    );

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).create(
        eq("Engineering"), eq("Engineering department"), eq("tenant-123"), eq("company-123")
    );
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getBody()).contains("new-dept-id");
    assertThat(response.getBody()).contains("Engineering");
  }

  @Test
  void shouldReturnBadRequestWhenCreateFails() {
    // Given
    String requestBody = "{\n" + "  \"name\": \"Engineering\",\n" + "  \"description\": \"Engineering department\",\n" + "  \"tenantId\": \"tenant-123\",\n" + "  \"companyId\": \"company-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("POST").withBody(requestBody);

    doReturn(Optional.empty()).when(departmentService).create(anyString(), anyString(), anyString(), anyString());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBody()).contains("Failed to create department");
  }

  @Test
  void shouldSuccessfullyUpdateDepartment() {
    // Given
    String departmentId = "existing-dept-id";
    String requestBody = "{\n" + "  \"name\": \"Updated Engineering\",\n" + "  \"description\": \"Updated description\",\n" + "  \"tenantId\": \"tenant-123\",\n" + "  \"companyId\": \"company-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("PUT").withBody(requestBody);

    Department updatedDepartment = new Department();
    updatedDepartment.setId(departmentId);
    updatedDepartment.setName("Updated Engineering");
    updatedDepartment.setDescription("Updated description");
    updatedDepartment.setTenantId("tenant-123");
    updatedDepartment.setCompanyId("company-123");

    doReturn(Optional.of(updatedDepartment)).when(departmentService).update(
        eq(departmentId), eq("Updated Engineering"), eq("Updated description"), eq("tenant-123"), eq("company-123")
    );

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).update(
        eq(departmentId), eq("Updated Engineering"), eq("Updated description"), eq("tenant-123"), eq("company-123")
    );
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBody()).contains("Updated Engineering");
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingNonExistentDepartment() {
    // Given
    String departmentId = "non-existent-id";
    String requestBody = "{\n" + "  \"name\": \"Updated Engineering\",\n" + "  \"description\": \"Updated description\",\n" + "  \"tenantId\": \"tenant-123\",\n" + "  \"companyId\": \"company-123\"\n" + "}";

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("PUT").withBody(requestBody);

    doReturn(Optional.empty()).when(departmentService).update(anyString(), anyString(), anyString(), anyString(), anyString());

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Department not found or update failed");
  }

  @Test
  void shouldSuccessfullyDeleteDepartment() {
    // Given
    String departmentId = "dept-to-delete";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("DELETE");

    Department department = new Department();
    department.setId(departmentId);
    department.setName("Engineering");
    department.setDescription("Engineering department");
    department.setTenantId("tenant-123");
    department.setCompanyId("company-123");

    doReturn(Optional.of(department)).when(departmentService).findById(departmentId);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findById(departmentId);
    verify(departmentService).deleteById(departmentId);
    assertThat(response.getStatusCode()).isEqualTo(204);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void shouldReturnNotFoundWhenDeletingNonExistentDepartment() {
    // Given
    String departmentId = "non-existent-id";
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments/" + departmentId).withHttpMethod("DELETE");

    doReturn(Optional.empty()).when(departmentService).findById(departmentId);

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(departmentService).findById(departmentId);
    verify(departmentService, never()).deleteById(anyString());
    assertThat(response.getStatusCode()).isEqualTo(404);
    assertThat(response.getBody()).contains("Department not found");
  }

  @Test
  void shouldReturnMethodNotAllowedForUnsupportedMethod() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("PATCH");

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(405);
    assertThat(response.getBody()).contains("Method Not Allowed");
  }

  @Test
  void shouldReturnInternalServerErrorOnException() {
    // Given
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withPath("/departments").withHttpMethod("GET");

    doThrow(new RuntimeException("Database error")).when(departmentService).findAll();

    // When
    APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

    // Then
    verify(lambdaLogger).log("Error in department endpoint: Database error");
    assertThat(response.getStatusCode()).isEqualTo(500);
    assertThat(response.getBody()).contains("Error processing department request");
  }
}
