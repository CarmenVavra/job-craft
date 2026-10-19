# Project Guide: Angular & Java Spring Boot Application

This document contains strict rules, guidelines, and commands that Claude MUST follow during development, testing, and automated code reviews

## 1. System Commands & Workflow - Before finalizing any task, creating a commit, or finishing a review, you must execute verification

- **Frontend Build & Test:** `npm run build` and `npm run test -- --watch=false`
- **Frontend Lint & Format:** `npm run lint` and `npm run format`
- **Backend Build & Test:** `./mvnw clean test` (or `./gradlew test`)
- **Backend Code Quality:** `./mvnw checkstyle:check`

CRITICAL: Never commit code that fails any of these steps. If a test fails, diagnose and fix it autonomously.

## 2. Automated Code Review Checklist

### Angular (Frontend) Guidelines

- **Architecture:** Use standalone components (Angular 14+). Enforce strict separation of UI (Components) and Business Logic/State (Services).
- **Reactivity:** Use RxJS streams efficiently. Always clean up subscriptions using `takeUntilDestroyed()`, `async` pipe, or `signals`. Avoid manual `.subscribe()` where possible.
- **State Management:** Prefer Angular Signals for local/component state.
- **Typing:** Strict TypeScript is mandatory. `any` is forbidden. Define explicit interfaces for all API payloads.

### Java Spring Boot (Backend) Guidelines

- **Architecture:** Enforce a strict 3-tier architecture: Controller -> Service -> Repository
- **DTO Pattern:** Never expose JPA Entities directly to the API layer. Use Records or DTOs for request/response bodies.
- **Error Handling:** Centralize API errors using `@RestControllerAdvice` and custom exceptions. No leaking of raw stack traces.
- **Null Safety:** Use `Optional<T>` for service returns that can be empty. Avoid returning raw `null`.
- **Database:** Ensure all Spring Data JPA queries are optimized (prevent N+1 query problems using `@EntityGraph` or `JOIN FETCH`).

### General Testing Requirements

- **Frontend:** Write Unit Tests with Jasmine/Karma or Jest for all new services and components.
- **Backend:** Write JUnit 5 tests. Use `@WebMvcTest` for controller slices and `@DataJpaTest` for repository slices. Integration tests must use `@SpringBootTest`.

## 3. Naming & Style Conventions

- **Angular:** Follow the official Angular Style Guide (e.g., `feature.component.ts`, `data.service.ts`).
- **Java:** Standard Java conventions (camelCase for variables, PascalCase for classes). Annotations must be neatly stacked above class/method declarations.

## 4. Git & Commit Guidelines

- **Format:** Conventional Commits (e.g., `feat(frontend): implement auth guard`, `fix(backend): fix user mapping`).

## 5. General

- **Sign-up/Registration:** If there is a sign-up/registration function, ensure that the user also has the option to change their password.
- **start.bat**: Create a start.bat in projectfolder for starting the app - frontend and backend
- **stop.bat**: Create a stop.bat in projectfolder for stopping the app - frontend and backend.
