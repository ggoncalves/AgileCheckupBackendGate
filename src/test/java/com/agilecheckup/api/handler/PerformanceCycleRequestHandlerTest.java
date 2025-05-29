package com.agilecheckup.api.handler;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.persistency.entity.PerformanceCycle;
import com.agilecheckup.service.PerformanceCycleService;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PerformanceCycleRequestHandlerTest {

    @Mock
    private ServiceComponent mockServiceComponent;

    @Mock
    private PerformanceCycleService mockPerformanceCycleService;

    @Mock
    private Context mockContext;

    @Mock
    private PaginatedScanList<PerformanceCycle> mockPaginatedScanList;

    private PerformanceCycleRequestHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        when(mockServiceComponent.buildPerformanceCycleService()).thenReturn(mockPerformanceCycleService);
        handler = new PerformanceCycleRequestHandler(mockServiceComponent, objectMapper);
    }

    @Test
    void testGetAllPerformanceCycles() {
        // Prepare
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles")
                .withHttpMethod("GET");

        PerformanceCycle cycle1 = PerformanceCycle.builder()
                .id("1")
                .name("Q1 2024")
                .description("First quarter")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        PerformanceCycle cycle2 = PerformanceCycle.builder()
                .id("2")
                .name("Q2 2024")
                .description("Second quarter")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true)
                .startDate(new Date())
                .endDate(new Date())
                .build();

        List<PerformanceCycle> cyclesList = Arrays.asList(cycle1, cycle2);
        when(mockPaginatedScanList.iterator()).thenReturn(cyclesList.iterator());
        when(mockPaginatedScanList.size()).thenReturn(cyclesList.size());
        when(mockPerformanceCycleService.findAll()).thenReturn(mockPaginatedScanList);

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(200, response.getStatusCode());
        verify(mockPerformanceCycleService).findAll();
    }

    @Test
    void testGetPerformanceCycleById() {
        // Prepare
        String cycleId = "123";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("GET");

        PerformanceCycle cycle = PerformanceCycle.builder()
                .id(cycleId)
                .name("Annual Review 2024")
                .description("Annual performance review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        when(mockPerformanceCycleService.findById(cycleId)).thenReturn(Optional.of(cycle));

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

        when(mockPerformanceCycleService.findById(cycleId)).thenReturn(Optional.empty());

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(404, response.getStatusCode());
        assertEquals("Performance cycle not found", response.getBody());
    }

    @Test
    void testCreatePerformanceCycleWithDates() throws Exception {
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

        PerformanceCycle createdCycle = PerformanceCycle.builder()
                .id("new-id")
                .name("Q3 2024")
                .description("Third quarter review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(true) // Should be true because endDate is present
                .startDate(new Date())
                .endDate(new Date())
                .build();

        when(mockPerformanceCycleService.create(
                eq("Q3 2024"),
                eq("Third quarter review"),
                eq("tenant1"),
                eq("company1"),
                eq(true),
                eq(false),
                any(Date.class),
                any(Date.class)
        )).thenReturn(Optional.of(createdCycle));

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("Q3 2024"),
                eq("Third quarter review"),
                eq("tenant1"),
                eq("company1"),
                eq(true),
                eq(false),
                any(Date.class),
                any(Date.class)
        );
    }

    @Test
    void testCreatePerformanceCycleWithoutDates() throws Exception {
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

        PerformanceCycle createdCycle = PerformanceCycle.builder()
                .id("new-id")
                .name("Ongoing Review")
                .description("Continuous performance review")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false) // Should be false because no endDate
                .build();

        when(mockPerformanceCycleService.create(
                eq("Ongoing Review"),
                eq("Continuous performance review"),
                eq("tenant1"),
                eq("company1"),
                eq(true),
                eq(true),
                isNull(),
                isNull()
        )).thenReturn(Optional.of(createdCycle));

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(201, response.getStatusCode());
        verify(mockPerformanceCycleService).create(
                eq("Ongoing Review"),
                eq("Continuous performance review"),
                eq("tenant1"),
                eq("company1"),
                eq(true),
                eq(true),
                isNull(),
                isNull()
        );
    }

    @Test
    void testUpdatePerformanceCycle() throws Exception {
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

        PerformanceCycle updatedCycle = PerformanceCycle.builder()
                .id(cycleId)
                .name("Updated Q1 2024")
                .description("Updated first quarter")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(false)
                .isTimeSensitive(true)
                .startDate(new Date())
                .endDate(new Date())
                .build();

        when(mockPerformanceCycleService.update(
                eq(cycleId),
                eq("Updated Q1 2024"),
                eq("Updated first quarter"),
                eq("tenant1"),
                eq("company1"),
                eq(false),
                eq(true),
                any(Date.class),
                any(Date.class)
        )).thenReturn(Optional.of(updatedCycle));

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(200, response.getStatusCode());
        verify(mockPerformanceCycleService).update(
                eq(cycleId),
                eq("Updated Q1 2024"),
                eq("Updated first quarter"),
                eq("tenant1"),
                eq("company1"),
                eq(false),
                eq(true),
                any(Date.class),
                any(Date.class)
        );
    }

    @Test
    void testUpdatePerformanceCycleNotFound() throws Exception {
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

        when(mockPerformanceCycleService.update(
                eq(cycleId),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyBoolean(),
                any(),
                any()
        )).thenReturn(Optional.empty());

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

        PerformanceCycle cycle = PerformanceCycle.builder()
                .id(cycleId)
                .name("To Delete")
                .description("Cycle to be deleted")
                .tenantId("tenant1")
                .companyId("company1")
                .isActive(true)
                .isTimeSensitive(false)
                .build();

        when(mockPerformanceCycleService.findById(cycleId)).thenReturn(Optional.of(cycle));

        // Execute
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Verify
        assertEquals(204, response.getStatusCode());
        verify(mockPerformanceCycleService).findById(cycleId);
        verify(mockPerformanceCycleService).delete(cycle);
    }

    @Test
    void testDeletePerformanceCycleNotFound() {
        // Prepare
        String cycleId = "nonexistent";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPath("/performancecycles/" + cycleId)
                .withHttpMethod("DELETE");

        when(mockPerformanceCycleService.findById(cycleId)).thenReturn(Optional.empty());

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
}