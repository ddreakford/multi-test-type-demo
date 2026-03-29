# Test Automation Demo

A multi-service test automation demo targeting the **Restful-Booker Platform** — a Docker-based hotel booking application with REST APIs and a web UI.

## Stack

| Layer | Tool |
|---|---|
| System Under Test | [Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform) (Docker Compose) |
| API Testing | RestAssured + TestNG |
| UI Testing | Selenium WebDriver + TestNG |
| Build Tool | Gradle (Groovy DSL) |
| Reporting | Allure Report |
| CI (Optional) | GitHub Actions |

## Prerequisites

- JDK 21 (configured via `gradle.properties`)
- Docker Desktop (with Docker Compose)
- Google Chrome (latest stable)
- Git

## Quick Start

```bash
# 1. Clone with submodules
git clone --recurse-submodules <repo-url>

# 2. Start the system under test
cd restful-booker-platform
docker compose up -d
cd ..

# 3. Wait for services to initialise (~15-20 seconds), then run the test suite
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
├── restful-booker-platform/   # System under test (git submodule)
└── rbp-test-demo/             # Gradle test project (test automation code)
```

## Acknowledgements

The system under test is the **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)** by [Mark Winteringham](https://github.com/mwinteringham). It is an open-source, multi-service hotel booking application built specifically for test automation training. It is included in this repository as a Git submodule — all credit for its design and implementation belongs to Mark Winteringham and its contributors.

- **Repository:** https://github.com/mwinteringham/restful-booker-platform
- **License:** [GPL-3.0](https://github.com/mwinteringham/restful-booker-platform/blob/master/LICENSE)
- **Author's site:** https://www.mwtestconsultancy.co.uk
