# RouteGuard Coding Standards Baseline

This document establishes the coding standards and best practices for the RouteGuard project. All contributors are expected to follow these guidelines to maintain code quality, consistency, and maintainability.

## Table of Contents
1. [General Principles](#general-principles)
2. [JavaScript/Node.js Standards](#javascriptnodejs-standards)
3. [SQL Standards](#sql-standards)
4. [Android/Kotlin Standards](#androidkotlin-standards)
5. [Git Standards](#git-standards)
6. [Security Standards](#security-standards)
7. [Testing Standards](#testing-standards)
8. [Documentation Standards](#documentation-standards)

---

## General Principles

### 1.1 Readability Over Cleverness
Code should be easy to read and understand. Prioritize clarity over clever shortcuts.

### 1.2 Consistency
Follow established patterns in the codebase. When in doubt, mimic existing code.

### 1.3 DRY Principle
Don't Repeat Yourself. Extract reusable code into functions, modules, or utilities.

### 1.4 Single Responsibility
Each function, class, or module should have one clear responsibility.

### 1.5 Error Handling
Always handle errors appropriately. Never ignore exceptions.

### 1.6 Security First
Consider security implications in all code. Follow the principle of least privilege.

---

## JavaScript/Node.js Standards

### 2.1 Language Features
- Use ES2022+ features (async/await, optional chaining, nullish coalescing)
- Prefer `const` and `let` over `var`
- Use arrow functions for concise callbacks
- Use template literals for string interpolation

### 2.2 Code Formatting
- Use 2-space indentation (no tabs)
- Maximum line length: 100 characters
- Use semicolons
- Trailing commas in multi-line objects/arrays
- Space after keywords (`if (condition)`, `function name()`)
- Space around operators (`x + y`, `a === b`)

### 2.3 Naming Conventions
- `camelCase` for variables, functions, and method names
- `PascalCase` for classes and constructors
- `UPPER_SNAKE_CASE` for constants
- Descriptive names: `getUserByEmail` not `getUser`
- Boolean variables: `isActive`, `hasPermission`, `shouldLoad`

### 2.4 Code Organization
- One class per file (when exporting a class)
- Related functions grouped together
- Public API at the top of the file
- Private helper functions at the bottom
- Use JSDoc comments for all public functions

### 2.5 Error Handling
- Use try/catch for asynchronous operations
- Validate all inputs
- Throw meaningful error messages
- Don't expose internal errors to users (use generic messages where appropriate)
- Log errors appropriately for debugging

### 2.6 Security
- Never log sensitive data (passwords, tokens, PII)
- Use parameterized queries to prevent SQL injection
- Validate and sanitize all user inputs
- Use environment variables for secrets
- Implement rate limiting where appropriate
- Use secure headers (helmet.js equivalent)

---

## SQL Standards

### 3.1 Schema Design
- Use UUIDs for primary keys (`uuid_generate_v4()`)
- Use `TIMESTAMPTZ` for timestamps
- Use appropriate constraints (CHECK, NOT NULL, UNIQUE)
- Use ENUM types for fixed-value columns
- Index foreign keys and frequently queried columns
- Use PostGIS GEOGRAPHY type for spatial data
- Use meaningful constraint names

### 3.2 Query Writing
- Use parameterized queries (never string concatenation)
- Use meaningful aliases for table names
- Explicitly list columns (avoid `SELECT *` in production code)
- Use CTEs (WITH clauses) for complex queries
- Use proper JOIN syntax (INNER JOIN, LEFT JOIN, etc.)
- Use transactions for related operations
- Comment complex queries

### 3.3 Naming Conventions
- Snake_case for table and column names
- Prefix boolean columns with `is_` or `has_`
- Use `_count` suffix for counter columns
- Use `_at` suffix for timestamp columns
- Use `_id` suffix for foreign key columns

### 3.4 Migrations
- Each migration should be reversible
- Make one logical change per migration
- Include both up and down migrations when possible
- Test migrations on a copy of production data
- Don't modify existing data types without careful consideration

---

## Android/Kotlin Standards

### 4.1 Language Features
- Use Kotlin 1.7+
- Prefer `val` over `var`
- Use data classes for simple data holders
- Use sealed classes for state representation
- Use coroutines for asynchronous work
- Use Kotlin flows for reactive streams
- Use when expressions instead of multiple if/else

### 4.2 Code Formatting
- Use 4-space indentation
- Maximum line length: 120 characters
- No semicolons
- Space around operators
- Blank lines between logical sections
- Import ordering: Kotlin, Android, third-party, local

### 4.3 Naming Conventions
- `camelCase` for variables, functions, and properties
- `PascalCase` for classes, interfaces, and objects
- `UPPER_SNAKE_CASE` for constants
- Prefix interfaces with `I` (optional, follow existing pattern)
- Descriptive names: `calculateDistance` not `calc`
- Boolean variables: `isVisible`, `hasData`, `shouldShow`

### 4.4 Architecture
- Follow MVVM (Model-View-ViewModel) pattern
- Use Hilt for dependency injection
- Use Repository pattern for data access
- Use ViewModels to manage UI-related data
- Use LiveData or StateFlow for UI state
- Keep Activities/Fragments thin (UI logic only)
- Business logic in ViewModels or Use Cases

### 4.5 UI Development
- Use Jetpack Compose for new UI development
- For XML layouts: use ConstraintLayout as root
- Use material design components
- Provide content descriptions for accessibility
- Use dimension resources for spacing
- Use color/theme resources for colors
- Use string resources for all user-facing text
- Use vector drawables for icons

### 4.6 Resource Management
- Use meaningful resource names (`feature_element_type`)
- Group resources by type and feature
- Use plurals for quantity-specific strings
- Use styles and themes for consistent appearance
- Use dimension resources for consistent spacing
- Use color state lists for interactive elements

### 4.7 Security
- Use Keystore for cryptographic operations
- Use EncryptedSharedPreferences or DataStore for sensitive data
- Never hardcode secrets or keys
- Use HTTPS for all network communications
- Validate all input from network/intents
- Use proper permission handling (request at runtime)
- Use Network Security Config for cleartext traffic

---

## Git Standards

### 5.1 Commit Messages
- Use conventional commits format: `type(scope): description`
- Types: feat, fix, docs, style, refactor, perf, test, chore
- Scope: optional, indicates affected area (e.g., auth, reports, ui)
- Description: imperative mood, max 50 characters
- Body: optional, wrap at 72 characters
- Footer: optional, for breaking changes or references

Examples:
```
feat(auth): add refresh token rotation
fix(reports): correct PostGIS query syntax
docs: update API documentation for endpoints
refactor(ui): simplify login screen navigation
```

### 5.2 Branching Strategy
- `main` branch: production-ready code
- Feature branches: `feature/short-description`
- Bug fix branches: `bugfix/issue-number-description`
- Release branches: `release/vX.Y.Z`
- Hotfix branches: `hotfix/description`

### 5.3 Pull Requests
- Keep PRs small and focused
- Write clear PR descriptions
- Reference related issues
- Request reviews from relevant team members
- Ensure all checks pass before merging
- Squash and merge for feature branches

### 5.4 Code Reviews
- Review for correctness, readability, and standards adherence
- Check for proper error handling
- Verify security considerations
- Test edge cases
- Look for potential performance issues
- Ensure proper testing

---

## Security Standards

### 6.1 Authentication & Authorization
- Use JWT for access tokens (15-minute expiration)
- Use refresh tokens with rotation (30-day expiration, SHA-256 hashed)
- Hash passwords with bcrypt (cost factor 12)
- Never store sensitive data in plaintext
- Implement proper role-based access control (RBAC)
- Validate user permissions on every protected endpoint
- Use owner-or-admin checks where appropriate
- Implement rate limiting on authentication endpoints

### 6.2 Data Protection
- Use parameterized queries to prevent SQL injection
- Validate and sanitize all user inputs
- Use HTTPS/TLS for all communications
- Encrypt sensitive data at rest when appropriate
- Use secure random generators for tokens
- Implement proper CORS policies
- Use security headers (helmet.js equivalent)

### 6.3 Privacy
- Minimize data collection (only collect what's necessary)
- Anonymize or pseudonymize data when possible
- Provide clear privacy notices
- Implement data retention policies
- Allow users to delete their data
- Securely handle location data (fuzz when appropriate)

---

## Testing Standards

### 7.1 Test Organization
- Unit tests: test individual functions/methods
- Integration tests: test interactions between components
- E2E tests: test critical user flows
- Place tests alongside implementation (`*.test.js` or `*Test.kt`)
- Use descriptive test names: `shouldReturnErrorWhenInvalidInput`

### 7.2 JavaScript/Node.js Testing
- Use Jest for unit and integration tests
- Use Supertest for API endpoint testing
- Mock external dependencies
- Test both success and failure paths
- Use beforeEach/afterEach for setup/teardown
- Aim for 80%+ code coverage on critical paths

### 7.3 Android/Kotlin Testing
- Use JUnit and Mockito for unit tests
- Use Espresso for UI tests
- Use coroutines-test for testing asynchronous code
- Test ViewModels with fake repositories
- Use @VisibleForTesting when necessary
- Test edge cases and error conditions

### 7.4 Test Quality
- Tests should be independent and repeatable
- Tests should run quickly (avoid sleeps, use mocking)
- Tests should be readable and maintainable
- Tests should verify behavior, not implementation details
- Use AAA pattern: Arrange, Act, Assert
- Don't test private methods directly (test through public API)

---

## Documentation Standards

### 8.1 Code Documentation
- Use JSDoc for JavaScript/KDoc for Kotlin
- Document all public APIs
- Include parameter descriptions, return values, and exceptions
- Use @see for related functions/methods
- Document complex algorithms and business logic
- Keep documentation updated with code changes

### 8.2 API Documentation
- Document all endpoints with method, path, parameters, and responses
- Include authentication requirements
- Include example requests and responses
- Document error codes and messages
- Use OpenAPI/Swagger specification when possible
- Keep API documentation in sync with implementation

### 8.3 Architectural Documentation
- Document key architectural decisions
- Maintain up-to-date ERD/database schema documentation
- Document data flow for critical processes
- Document third-party integrations
- Document deployment and environment setup

### 8.4 README Files
- Each major component should have a README
- Include setup instructions
- Include usage examples
- Include contribution guidelines
- Include licensing information

---
*Last updated: 2026-08-07*
*This document should evolve with the project. Update it when standards change or new best practices are adopted.*