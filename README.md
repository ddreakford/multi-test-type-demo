# Test Automation Demo

A multi-service test automation demo targeting the **Restful-Booker Platform** — a Docker-based hotel booking application with REST APIs and a web UI.

## Stack

| Layer | Tool |
|---|---|
| System Under Test | Restful-Booker Platform (Docker) |
| API Testing | RestAssured + TestNG |
| UI Testing | Selenium WebDriver + TestNG |
| Build Tool | Gradle (Groovy DSL) |
| Reporting | Allure Report |
| CI (Optional) | GitHub Actions |

## Prerequisites

- JDK 17+
- Docker Desktop
- Google Chrome (latest stable)
- Git

## Quick Start

```bash
# 1. Start the system under test
docker run -d --name rbp -p 3003:3003 mwinteringham/restfulbooker-platform:latest

# 2. Verify services are up
curl http://localhost:3003/booking/

# 3. Run the test suite (from rbp-test-demo/)
cd rbp-test-demo
./gradlew clean test

# 4. View the Allure report
./gradlew allureServe
```

## Documentation

See [docs/TestAutomationDemo_SetupGuide.md](docs/TestAutomationDemo_SetupGuide.md) for the full step-by-step setup and execution guide.

To generate a `.docx` version for sharing:
```bash
./scripts/convert-to-docx.sh
```

## Project Structure

```
test-automation-demo/
├── CLAUDE.md                  # Project conventions for Claude Code
├── README.md
├── docs/                      # Setup guide (.md source + .docx for sharing)
├── scripts/                   # Utility scripts
└── rbp-test-demo/             # Gradle test project (created during setup)
```
