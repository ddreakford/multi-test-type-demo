# Test Automation Demo — Project Conventions

## Overview
Multi-service test automation demo targeting the Restful-Booker Platform (Docker).
Stack: Java 17, Gradle, TestNG, RestAssured, Selenium WebDriver, Allure Report.

## Project Structure
- `docs/` — Setup guide (authoritative source is `.md`; `.docx` is generated for sharing)
- `scripts/` — Utility scripts (e.g., `convert-to-docx.sh`)
- `rbp-test-demo/` — Gradle project with test source code (created during setup)

## Conventions
- **Markdown is the source of truth** for documentation. Use `scripts/convert-to-docx.sh` to produce `.docx` for sharing.
- **Java 17** minimum. Use TestNG annotations for test lifecycle.
- **Allure annotations** (`@Epic`, `@Feature`, `@Story`, `@Severity`) on all test classes/methods.
- **RestAssured** for API tests; **Selenium WebDriver** for UI tests.
- **Docker**: The system under test runs via `docker run -d --name rbp -p 3003:3003 mwinteringham/restfulbooker-platform:latest`.

## Git Workflow
- Branch: `main`
- Remote will be set up on GitHub after the first local working version.
- Commit messages: concise, imperative mood.

## Useful Commands
```bash
# Convert Markdown guide to .docx
./scripts/convert-to-docx.sh

# Start system under test
docker start rbp

# Run tests (from rbp-test-demo/)
./gradlew clean test

# Open Allure report
./gradlew allureServe
```
