# Gemini's Best Practices Guide

This document summarizes the development best practices and architectural rules we have established for this project.

## 1. API Design (REST Principles)

- **Use Nouns for Resources:** API endpoints should represent resources (nouns), not actions (verbs).
  - *Example:* Use `GET /setup` to retrieve the setup status, not `GET /check-initialize`.
- **Use HTTP Verbs for Actions:** Use standard HTTP methods to operate on resources.
  - `GET`: Retrieve a resource.
  - `POST`: Create a new resource.
  - `PUT` / `PATCH`: Update an existing resource.
  - `DELETE`: Remove a resource.
- **Endpoint Consistency:** Keep endpoint styles consistent (e.g., `application/x-www-form-urlencoded` or JSON payloads) for similar types of actions.

## 2. API Documentation (Swagger)

- **Document Every Endpoint:** All public API endpoints must be documented using Swagger annotations to ensure the API is self-describing.
- **Operation Summary:** Use `@Operation(summary = "...")` to provide a clear, human-readable description of what the endpoint does.
- **Response Descriptions:** Use `@ApiResponse` to document all possible responses for an endpoint, including success (2xx) and error (4xx, 5xx) scenarios.
  - *Example:* `@ApiResponse(responseCode = "200", description = "Successful operation")`
  - *Example:* `@ApiResponse(responseCode = "404", description = "Project not found")`

## 3. Code Architecture & Separation of Concerns

- **Service Layer:** All business logic should be encapsulated within a service layer (e.g., `UserService`). But don't create a service just to call a Repository. A Repository can be called directly from a Controller if there's no business logic. A service should not have "REST API logic" with HTTP status codes or web-specific classes.
- **Controller Layer:** Controllers should be lean. Their only responsibility is to handle HTTP requests, call the appropriate service methods, and return an HTTP response. They should not contain business logic.
- **Repository Layer:** Repositories are responsible for data access only.
- **No Web Dependencies in Services:** The service layer must remain framework-agnostic. It should not have any dependencies on web-layer classes (e.g., `ResponseEntity`, `HttpServletRequest`).

## 4. Data Transfer Objects (DTOs)

- **Dedicated DTOs:** Use specific DTOs for API requests and responses (e.g., `LoginRequest`, `ErrorResponse`). 
- **Package Organization:**
  - DTOs belong in the `fr.grozeille.db4all.api.dto` package.
  - JPA entities and business models belong in the `fr.grozeille.db4all.model` package.
- **API-Specific Annotations:** Use annotations like `@Schema(format = "password")` in DTOs to provide hints to API documentation tools like Swagger UI.
- **No usage of DTO in the service layer**: DTOs are used only for API requests and responses. They should not be used in the service layer, which should work with business models or entities directly. If needed, use a Mapping library to map the DTP to a model object. Ex: we don't need the ID to create a Project, so use a `ProjectCreateRequest` DTO without an ID field, and map it to a `Project` entity in the service layer.

## 5. Error Handling and Logging

- **Return Detailed Errors:** In addition to a standard HTTP error code (like 400 or 404), the response body **must** contain a consistent JSON `ErrorResponse` object with a clear, human-readable message. This provides more context to the API consumer than an HTTP status code alone.
- **Log Exceptions:** When an exception is caught and not re-thrown, always log the full exception stack trace. This is critical for debugging.
- **Consistent Error Responses:** Use a single, consistent error response object (`ErrorResponse`) for all error scenarios across the API.
- **Log in English:** All log messages must be written in English to ensure they are universally understandable by any developer.
