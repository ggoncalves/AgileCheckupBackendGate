package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.PerformanceCycleV2;
import com.agilecheckup.service.PerformanceCycleService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PerformanceCycleRequestHandlerTest {

    @Mock
    private ServiceComponent mockServiceComponent;

    @Mock
    private PerformanceCycleService mockPerformanceCycleService;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLambdaLogger;

    private PerformanceCycleRequestHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.registerModule(new JavaTimeModule());
        lenient().doReturn(mockPerformanceCycleService).when(mockServiceComponent).buildPerformanceCycleService();
        lenient().doReturn(mockLambdaLogger).when(mockContext).getLogger();
        handler = new PerformanceCycleRequestHandler(mockServiceComponent, objectMapper);
    }

    @Test
    void testGetAllPerformanceCyclesWithTenantId() {
        // Prepare
        String tenantId = "tenant1";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
            .withHttpMethod("GET")
            .withQueryStringParameters(java.util.Collections.singletonMap("tenantId", tenantId));

        PerformanceCycleV2 cycle1 = PerformanceCycleV2.builder()
                .id("1")
                .name("Q1 2024")
                .description("First quarter")
            .tenantId(tenantId)
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        PerformanceCycleV2 cycle2 = PerformanceCycleV2.builder()
                .id("2")
                .name("Q2 2024")
                .description("Second quarter")
            .tenantId(tenantId)
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();

        List<PerformanceCycleV2> cyclesList = Arrays.asList(cycle1, cycle2);
        doReturn(cyclesList).when(mockPerformanceCycleService).findAllByTenantId(tenantId);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(200, response.getStatusCode());
        verify(mockPerformanceCycleService).findAllByTenantId(tenantId);
    }

    @Test
    void testGetAllPerformanceCyclesWithoutTenantId() {
        // Prepare
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
            .withPath("/performancecycles")
            .withHttpMethod("GET");

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(400, response.getStatusCode());
        assertEquals("tenantId is required", response.getBody());
    }

    @Test
    void testGetPerformanceCycleById() {
        // Prepare
        String cycleId = "123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("GET");

        PerformanceCycleV2 cycle = PerformanceCycleV2.builder()
                .id(cycleId)
                .name("Annual Review 2024")
                .description("Annual performance review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        doReturn(Optional.of(cycle)).when(mockPerformanceCycleService).findById(cycleId);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(200, response.getStatusCode());
        verify(mockPerformanceCycleService).findById(cycleId);
    }

    @Test
    void testGetPerformanceCycleByIdNotFound() {
        // Prepare
        String cycleId = "nonexistent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("GET");

        doReturn(Optional.empty()).when(mockPerformanceCycleService).findById(cycleId);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(404, response.getStatusCode());
        assertEquals("Performance cycle not found", response.getBody());
    }

    @Test
    void testCreatePerformanceCycleWithDates() {
        // Prepare
        String requestBody = "{"
                + "\"name\":\"Q3 2024\","
                + "\"description\":\"Third quarter review\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":false,"
                + "\"startDate\":\"2024-07-01\","
                + "\"endDate\":\"2024-09-30\""
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("POST")
                .withBody(requestBody);

        PerformanceCycleV2 createdCycle = PerformanceCycleV2.builder()
                .id("new-id")
                .name("Q3 2024")
                .description("Third quarter review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true) // Should be true because endDate is present
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();

        doReturn(Optional.of(createdCycle)).when(mockPerformanceCycleService).create(
                eq("tenant1"),
                eq("Q3 2024"),
                eq("Third quarter review"),
                eq("company1"),
                eq(true),
                eq(false),
                any(LocalDate.class),
                any(LocalDate.class)
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("tenant1"),
                eq("Q3 2024"),
                eq("Third quarter review"),
                eq("company1"),
                eq(true),
                eq(false),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    void testCreatePerformanceCycleWithoutDates() {
        // Prepare
        String requestBody = "{"
                + "\"name\":\"Ongoing Review\","
                + "\"description\":\"Continuous performance review\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":true"
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("POST")
                .withBody(requestBody);

        PerformanceCycleV2 createdCycle = PerformanceCycleV2.builder()
                .id("new-id")
                .name("Ongoing Review")
                .description("Continuous performance review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false) // Should be false because no endDate
                .build();

        doReturn(Optional.of(createdCycle)).when(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Ongoing Review"),
                eq("Continuous performance review"),
                eq("company1"),
                eq(true),
                eq(true),
                isNull(),
                isNull()
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Ongoing Review"),
                eq("Continuous performance review"),
                eq("company1"),
                eq(true),
                eq(true),
                isNull(),
                isNull()
        );
    }

    @Test
    void testUpdatePerformanceCycle() {
        // Prepare
        String cycleId = "123";
        String requestBody = "{"
                + "\"name\":\"Updated Q1 2024\","
                + "\"description\":\"Updated first quarter\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":false,"
                + "\"isTimeSensitive\":true,"
                + "\"startDate\":\"2024-01-01T00:00:00.000Z\","
                + "\"endDate\":\"2024-03-31T23:59:59.999Z\""
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        PerformanceCycleV2 updatedCycle = PerformanceCycleV2.builder()
                .id(cycleId)
                .name("Updated Q1 2024")
                .description("Updated first quarter")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(false)
                .isTimeSensitive(true)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();

        doReturn(Optional.of(updatedCycle)).when(mockPerformanceCycleService).update(
                eq(cycleId),
                eq("tenant1"),
                eq("Updated Q1 2024"),
                eq("Updated first quarter"),
                eq("company1"),
                eq(false),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(200, response.getStatusCode());
        verify(mockPerformanceCycleService).update(
                eq(cycleId),
                eq("tenant1"),
                eq("Updated Q1 2024"),
                eq("Updated first quarter"),
                eq("company1"),
                eq(false),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    void testUpdatePerformanceCycleNotFound() {
        // Prepare
        String cycleId = "nonexistent";
        String requestBody = "{"
                + "\"name\":\"Updated\","
                + "\"description\":\"Updated\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":false"
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("PUT")
                .withBody(requestBody);

        doReturn(Optional.empty()).when(mockPerformanceCycleService).update(
                eq(cycleId),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyBoolean(),
                any(),
                any()
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(404, response.getStatusCode());
        assertEquals("Performance cycle not found or update failed", response.getBody());
    }

    @Test
    void testDeletePerformanceCycle() {
        // Prepare
        String cycleId = "123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("DELETE");

        PerformanceCycleV2 cycle = PerformanceCycleV2.builder()
                .id(cycleId)
                .name("To Delete")
                .description("Cycle to be deleted")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        doReturn(Optional.of(cycle)).when(mockPerformanceCycleService).findById(cycleId);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(204, response.getStatusCode());
        verify(mockPerformanceCycleService).findById(cycleId);
        verify(mockPerformanceCycleService).deleteById(cycleId);
    }

    @Test
    void testDeletePerformanceCycleNotFound() {
        // Prepare
        String cycleId = "nonexistent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("DELETE");

        doReturn(Optional.empty()).when(mockPerformanceCycleService).findById(cycleId);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(404, response.getStatusCode());
        assertEquals("Performance cycle not found", response.getBody());
    }

    @Test
    void testUnsupportedMethod() {
        // Prepare
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("PATCH");

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(405, response.getStatusCode());
        assertEquals("Method Not Allowed", response.getBody());
    }

    @Test
    void testCreatePerformanceCycleWithISO8601DateTime() {
        // Test the current parseDate method with ISO 8601 format with milliseconds and timezone
        String requestBody = "{"
                + "\"name\":\"Q4 2024\","
                + "\"description\":\"Fourth quarter review\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":true,"
                + "\"startDate\":\"2024-10-01T00:00:00.000Z\","
                + "\"endDate\":\"2024-12-31T23:59:59.999Z\""
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("POST")
                .withBody(requestBody);

        PerformanceCycleV2 createdCycle = PerformanceCycleV2.builder()
                .id("new-id")
                .name("Q4 2024")
                .description("Fourth quarter review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();

        doReturn(Optional.of(createdCycle)).when(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Q4 2024"),
                eq("Fourth quarter review"),
                eq("company1"),
                eq(true),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Q4 2024"),
                eq("Fourth quarter review"),
                eq("company1"),
                eq(true),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    void testCreatePerformanceCycleWithSimpleDateFormat() {
        // Test the current parseDate method with simple date format
        String requestBody = "{"
                + "\"name\":\"Q1 2025\","
                + "\"description\":\"First quarter 2025\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":true,"
                + "\"startDate\":\"2025-01-01\","
                + "\"endDate\":\"2025-03-31\""
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("POST")
                .withBody(requestBody);

        PerformanceCycleV2 createdCycle = PerformanceCycleV2.builder()
                .id("new-id")
                .name("Q1 2025")
                .description("First quarter 2025")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .build();

        doReturn(Optional.of(createdCycle)).when(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Q1 2025"),
                eq("First quarter 2025"),
                eq("company1"),
                eq(true),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Q1 2025"),
                eq("First quarter 2025"),
                eq("company1"),
                eq(true),
                eq(true),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    void testCreatePerformanceCycleWithNullDates() {
        // Test the current parseDate method with null dates
        String requestBody = "{"
                + "\"name\":\"Open Review\","
                + "\"description\":\"Open-ended review\","
                + "\"tenantId\":\"tenant1\","
                + "\"companyId\":\"company1\","
                + "\"isActive\":true,"
                + "\"isTimeSensitive\":false"
                + "}";

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("POST")
                .withBody(requestBody);

        PerformanceCycleV2 createdCycle = PerformanceCycleV2.builder()
                .id("new-id")
                .name("Open Review")
                .description("Open-ended review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        doReturn(Optional.of(createdCycle)).when(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Open Review"),
                eq("Open-ended review"),
                eq("company1"),
                eq(true),
                eq(false),
                isNull(),
                isNull()
        );

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("tenant1"),  // V2 signature: tenantId first
                eq("Open Review"),
                eq("Open-ended review"),
                eq("company1"),
                eq(true),
                eq(false),
                isNull(),
                isNull()
        );
    }
}