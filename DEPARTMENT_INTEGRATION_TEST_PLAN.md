# Integration Test Plan: Department Endpoints for AgileCheckupBackendGate

## Table of Contents
1. [Introduction and Scope](#1-introduction-and-scope)
2. [Objectives](#2-objectives)
3. [Interaction with AgileCheckupPerpetua](#3-interaction-with-agilecheckupperpetua)
4. [Testing Environment and Tools](#4-testing-environment-and-tools)
   - [Core Testing Libraries](#41-core-testing-libraries)
   - [Test Execution](#42-test-execution)
5. [Test Strategy for Department Endpoints](#5-test-strategy-for-department-endpoints)
   - [GET /departments](#51-get-departments)
   - [GET /departments/{id}](#52-get-departmentsid)
   - [POST /departments](#53-post-departments)
   - [PUT /departments/{id}](#54-put-departmentsid)
   - [DELETE /departments/{id}](#55-delete-departmentsid)
6. [Mocking Strategy](#6-mocking-strategy)
   - [Rationale for Mocking](#61-rationale-for-mocking)
   - [Implementation with Mockito](#62-implementation-with-mockito)

---

## 1. Introduction and Scope

This document outlines the plan for creating integration tests for the Department CRUD (Create, Read, Update, Delete) endpoints located within the `AgileCheckupBackendGate` repository.

The primary scope of these integration tests is to verify the correct behavior of the `DepartmentRequestHandler` in `AgileCheckupBackendGate`. This includes:
- Routing of requests.
- Request and response handling.
- Interaction with the `DepartmentService`.

## 2. Objectives

The main objectives of these integration tests are:
- To ensure that each Department API endpoint (`GET /departments`, `GET /departments/{id}`, `POST /departments`, `PUT /departments/{id}`, `DELETE /departments/{id}`) behaves as expected under various conditions.
- To verify that the `DepartmentRequestHandler` correctly processes input, invokes the `DepartmentService` with appropriate parameters, and formats the responses accurately.
- To provide confidence in the stability and correctness of the Department functionality within the `AgileCheckupBackendGate` service.

## 3. Interaction with AgileCheckupPerpetua

The `AgileCheckupBackendGate` service utilizes `DepartmentService` from the `AgileCheckupPerpetua` library for data persistence and business logic related to departments. Since `AgileCheckupPerpetua` involves interactions with AWS DynamoDB and is an external dependency, the integration tests described in this plan will **mock** the `DepartmentService` interface.

This approach allows us to:
- Isolate the `AgileCheckupBackendGate` logic for testing.
- Avoid dependencies on a live DynamoDB instance or the fully deployed `AgileCheckupPerpetua` service during these specific integration tests.
- Ensure test repeatability and stability.

## 4. Testing Environment and Tools

The integration tests will be developed and executed within the `AgileCheckupBackendGate` Maven project.

### 4.1. Core Testing Libraries

- **JUnit 5 (Jupiter):** The primary framework for writing and running tests. It provides annotations like `@Test`, `@BeforeEach`, `@AfterEach`, and assertion methods.
- **Mockito:** A mocking framework used to create test doubles (mocks) for dependencies, specifically the `DepartmentService`. This allows for controlled testing of the `DepartmentRequestHandler` in isolation.

### 4.2. Test Execution

- **Maven Surefire Plugin:** Tests will be executed as part of the standard Maven build lifecycle (`mvn test`).
- **Test Location:** Test classes will reside in the `src/test/java` directory, following a package structure similar to the code being tested (e.g., `com.agilecheckup.api.handler`).

## 5. Test Strategy for Department Endpoints

This section outlines the specific test strategies for each of the Department RESTful endpoints handled by `DepartmentRequestHandler`. The focus is on testing the handler's logic, its interaction with the mocked `DepartmentService`, and the responses it generates.

For each endpoint, tests will cover:
- Successful execution (happy path).
- Handling of common error conditions (e.g., resource not found).
- Correct parsing of path variables and query parameters.
- Appropriate HTTP status codes and response body formats.

### 5.1. GET /departments

- **Purpose:** Retrieves all departments, optionally filtered by `tenantId`.
- **Test Scenarios:**
    - **Scenario 1: Get All Departments (No Filter)**
        - **Mock Setup:** `departmentService.findAll()` returns a list of mock `Department` objects.
        - **Assertions:**
            - HTTP Status: 200 OK.
            - Response Body: JSON array of department objects.
            - `departmentService.findAll()` was called once.
    - **Scenario 2: Get All Departments (With `tenantId` Filter)**
        - **Mock Setup:** `departmentService.findAllByTenantId(testTenantId)` returns a list of mock `Department` objects specific to that tenant.
        - **Request:** Include `tenantId` as a query parameter.
        - **Assertions:**
            - HTTP Status: 200 OK.
            - Response Body: JSON array of department objects for the specified tenant.
            - `departmentService.findAllByTenantId(testTenantId)` was called once with the correct `tenantId`.
    - **Scenario 3: No Departments Found**
        - **Mock Setup:** `departmentService.findAll()` (or `findAllByTenantId`) returns an empty list.
        - **Assertions:**
            - HTTP Status: 200 OK.
            - Response Body: Empty JSON array `[]`.

### 5.2. GET /departments/{id}

- **Purpose:** Retrieves a specific department by its ID.
- **Test Scenarios:**
    - **Scenario 1: Get Department by ID (Found)**
        - **Mock Setup:** `departmentService.findById(testId)` returns `Optional.of(mockDepartment)`.
        - **Request:** Path variable `id` set to `testId`.
        - **Assertions:**
            - HTTP Status: 200 OK.
            - Response Body: JSON representation of the `mockDepartment`.
            - `departmentService.findById(testId)` was called once.
    - **Scenario 2: Department Not Found**
        - **Mock Setup:** `departmentService.findById(nonExistentId)` returns `Optional.empty()`.
        - **Request:** Path variable `id` set to `nonExistentId`.
        - **Assertions:**
            - HTTP Status: 404 Not Found.
            - Response Body: Error message indicating "Department not found".
            - `departmentService.findById(nonExistentId)` was called once.

### 5.3. POST /departments

- **Purpose:** Creates a new department.
- **Test Scenarios:**
    - **Scenario 1: Create Department Successfully**
        - **Request Body:** Valid JSON payload for a new department (e.g., `{"name": "New Dept", "description": "Desc", "tenantId": "t1", "companyId": "c1"}`).
        - **Mock Setup:** `departmentService.create(anyString(), anyString(), anyString(), anyString())` returns `Optional.of(createdDepartment)`.
        - **Assertions:**
            - HTTP Status: 201 Created.
            - Response Body: JSON representation of the `createdDepartment`.
            - `departmentService.create()` called once with parameters matching the request body.
    - **Scenario 2: Create Department Fails (Service Layer)**
        - **Request Body:** Valid JSON payload.
        - **Mock Setup:** `departmentService.create(anyString(), anyString(), anyString(), anyString())` returns `Optional.empty()`.
        - **Assertions:**
            - HTTP Status: 400 Bad Request (or as per current implementation for creation failure).
            - Response Body: Error message indicating failure.
            - `departmentService.create()` called once.
    - **Scenario 3: Invalid Request Body (e.g., missing required fields)**
        - **Request Body:** JSON payload missing required fields.
        - **Assertions:**
            - HTTP Status: 400 Bad Request (or appropriate error status from Jackson deserialization or handler logic before service call).
            - Response Body: Error message indicating bad request or missing fields.
            - `departmentService.create()` may not be called if validation fails early.

### 5.4. PUT /departments/{id}

- **Purpose:** Updates an existing department.
- **Test Scenarios:**
    - **Scenario 1: Update Department Successfully**
        - **Request Body:** Valid JSON payload for updating a department.
        - **Mock Setup:** `departmentService.update(eq(testId), anyString(), anyString(), anyString(), anyString())` returns `Optional.of(updatedDepartment)`.
        - **Request:** Path variable `id` set to `testId`.
        - **Assertions:**
            - HTTP Status: 200 OK.
            - Response Body: JSON representation of the `updatedDepartment`.
            - `departmentService.update()` called once with `testId` and other parameters from the request body.
    - **Scenario 2: Update Department Not Found**
        - **Request Body:** Valid JSON payload.
        - **Mock Setup:** `departmentService.update(eq(nonExistentId), anyString(), anyString(), anyString(), anyString())` returns `Optional.empty()`.
        - **Request:** Path variable `id` set to `nonExistentId`.
        - **Assertions:**
            - HTTP Status: 404 Not Found.
            - Response Body: Error message indicating "Department not found or update failed".
            - `departmentService.update()` called once.

### 5.5. DELETE /departments/{id}

- **Purpose:** Deletes a department by its ID.
- **Test Scenarios:**
    - **Scenario 1: Delete Department Successfully**
        - **Mock Setup:**
            - `departmentService.findById(testId)` returns `Optional.of(mockDepartment)`.
            - `departmentService.delete(mockDepartment)` completes successfully (void method).
        - **Request:** Path variable `id` set to `testId`.
        - **Assertions:**
            - HTTP Status: 204 No Content.
            - `departmentService.findById(testId)` called once.
            - `departmentService.delete(mockDepartment)` called once with the department found.
    - **Scenario 2: Delete Department Not Found**
        - **Mock Setup:** `departmentService.findById(nonExistentId)` returns `Optional.empty()`.
        - **Request:** Path variable `id` set to `nonExistentId`.
        - **Assertions:**
            - HTTP Status: 404 Not Found.
            - Response Body: Error message indicating "Department not found".
            - `departmentService.findById(nonExistentId)` called once.
            - `departmentService.delete()` is not called.

## 6. Mocking Strategy

As mentioned in the Introduction (Section 3), the `DepartmentService` (from the `AgileCheckupPerpetua` library) will be mocked for these integration tests. This is a critical aspect of the testing strategy for `AgileCheckupBackendGate`'s `DepartmentRequestHandler`.

### 6.1. Rationale for Mocking

- **Isolation:** Mocking allows testing of the `DepartmentRequestHandler` in isolation, without needing a running instance of `AgileCheckupPerpetua` or its dependencies (like DynamoDB).
- **Control:** It provides full control over the behavior of the `DepartmentService` during tests. We can simulate various scenarios, including successful operations, data not found, exceptions, or other service-level responses.
- **Speed and Reliability:** Tests that rely on external services or databases can be slow and brittle. Mocking ensures tests are fast and reliable.
- **Focus:** The primary goal is to test the logic within `AgileCheckupBackendGate` (the "Gate" layer), not the underlying `AgileCheckupPerpetua` (the "Perpetua" service layer). Unit and integration tests for `AgileCheckupPerpetua` should exist within its own repository.

### 6.2. Implementation with Mockito

- **Instantiation:** The `DepartmentRequestHandler` receives a `ServiceComponent` in its constructor, which in turn provides the `DepartmentService`. In the test setup, we will:
    1. Mock the `ServiceComponent`.
    2. Mock the `DepartmentService`.
    3. Configure the mocked `ServiceComponent` to return the mocked `DepartmentService` when `buildDepartmentService()` is called.
    4. Instantiate `DepartmentRequestHandler` with the mocked `ServiceComponent`.

- **Defining Mock Behavior:** Mockito's `when(...).thenReturn(...)` or `when(...).thenThrow(...)` constructs will be used to define the behavior of `DepartmentService` methods for each test case.

    **Example:**
    ```java
    // In the test class, assuming 'mockDepartmentService' is a @Mock field
    // and 'objectMapper' is available for serializing test data.

    // For GET /departments/{id} - Found
    Department mockDepartment = new Department(); // Populate with test data
    mockDepartment.setId("test-id");
    mockDepartment.setName("Test Department");
    // ... other properties

    when(mockDepartmentService.findById("test-id")).thenReturn(Optional.of(mockDepartment));

    // For GET /departments/{id} - Not Found
    when(mockDepartmentService.findById("non-existent-id")).thenReturn(Optional.empty());

    // For POST /departments - Successful creation
    Department newDepartmentData = new Department(); // Data from request
    newDepartmentData.setName("New Department");
    // ...
    Department createdDepartment = new Department(); // What the service returns
    createdDepartment.setId("newly-created-id");
    createdDepartment.setName("New Department");
    // ...

    when(mockDepartmentService.create(
        eq(newDepartmentData.getName()),
        anyString(), // or eq() for other fields if they are fixed
        anyString(),
        anyString()
    )).thenReturn(Optional.of(createdDepartment));
    ```

- **Verifying Interactions:** Mockito's `verify(...)` methods will be used to ensure that `DepartmentService` methods are called with the expected parameters and the correct number of times.

    **Example:**
    ```java
    // Verify findById was called once with "test-id"
    verify(mockDepartmentService, times(1)).findById("test-id");

    // Verify create was called with specific arguments
    verify(mockDepartmentService, times(1)).create(
        eq("Expected Name"),
        eq("Expected Description"),
        eq("Expected TenantId"),
        eq("Expected CompanyId")
    );
    ```
