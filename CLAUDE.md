# Multi Test Type Demo — Project Conventions

## Overview
Multi-service test automation demo targeting the Restful-Booker Platform (Docker).
Stack: Java 17, Gradle, TestNG, RestAssured, Selenium WebDriver, Allure Report.

## Project Structure
- `docs/` — Setup guide (authoritative source is `.md`; `.docx` is generated for sharing)
- `scripts/` — Utility scripts (e.g., `convert-to-docx.sh`)
- `restful-booker-platform/` — Cloned SUT repo, started via docker-compose
- `rbp-test-demo/` — Gradle project with test source code (created during setup)

## Conventions
- **Markdown is the source of truth** for documentation. Use `scripts/convert-to-docx.sh` to produce `.docx` for sharing.
- **Java 17** minimum. Use TestNG annotations for test lifecycle.
- **Allure annotations** (`@Epic`, `@Feature`, `@Story`, `@Severity`) on all test classes/methods.
- **RestAssured** for API tests; **Selenium WebDriver** for UI tests.
- **Docker Compose**: The SUT runs as 7 microservices via `cd restful-booker-platform && DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up -d`.
- **Service ports**: Auth=3004, Booking=3000, Room=3001, Branding=3002, Report=3005, Message=3006, UI=80.
- **Auth model**: Cookie-based tokens (`Set-Cookie: token=<value>`), NOT JSON body. Use `response.getCookie("token")` in RestAssured.

## Git Workflow
- Branch: `main`
- Remote will be set up on GitHub after the first local working version.
- Commit messages: concise, imperative mood.

## Useful Commands
```bash
# Convert Markdown guide to .docx
./scripts/convert-to-docx.sh

# Start system under test
cd restful-booker-platform && docker compose start

# First-time start / full reset (DOCKER_DEFAULT_PLATFORM for Apple Silicon)
cd restful-booker-platform && DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up -d

# Stop system under test
cd restful-booker-platform && docker compose stop

# Run tests (from rbp-test-demo/)
cd rbp-test-demo && ./gradlew clean test

# Open Allure report
./gradlew allureServe
```
