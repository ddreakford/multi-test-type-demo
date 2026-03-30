# Test Automation Demo

A multi-service test automation demo targeting the **Restful-Booker Platform** — a Docker-based hotel booking application with REST APIs and a web UI.

## Stack

| Layer | Tool |
|---|---|
| System Under Test | [Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform) (Docker Compose) |
| Build Tool | Gradle (Groovy DSL) |
| API Testing | RestAssured + TestNG |
| UI Testing | Selenium WebDriver + TestNG |
| Reporting | Allure Report |
| CI | GitHub Actions |

## Prerequisites

- JDK 21 (see [JDK Setup](#jdk-setup) below)
- Docker Desktop (with Docker Compose)
- Google Chrome (latest stable)
- Git

## Quick Start

```bash
# 1. Clone with submodules (including the SUT repo, restful-booker-platform)
git clone --recurse-submodules https://github.com/ddreakford/test-automation-demo.git
cd test-automation-demo

# 2. Start the system under test (DOCKER_DEFAULT_PLATFORM needed on Apple Silicon)
cd restful-booker-platform
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up -d
cd ..

# 3. Wait ~20 seconds for services to initialise, then run the test suite
cd rbp-test-demo
./gradlew clean test

# 4. View the Allure report
./gradlew allureServe
```

> **NOTE:** If your system JDK is newer than 21 (e.g., JDK 25), Gradle may fail. See [JDK Setup](#jdk-setup) below.

## What's in the Test Suite

This project includes three categories of tests, plus an RCA demonstration:

### Automated Tests (23 tests — run locally and in CI)

| Suite | Tests | What It Covers |
|---|---|---|
| **API Tests** (Auth, Booking CRUD) | 5 | Cookie-based auth, create/read/delete bookings via REST |
| **UI Test** (Admin Login) | 1 | Browser-based admin panel login with Selenium |
| **Regression Suite** (Homepage, Booking Flow, Contact Form) | 17 | Full customer-facing UI validation — maps 1:1 to the manual test scenarios |

```bash
# Run all 23 automated tests
./gradlew clean test

# View Allure report with results
./gradlew allureServe
```

### RCA Demo (2 intentional failures — for Allure dashboard practice)

Two tests that are **designed to fail** to demonstrate root cause analysis in Allure:

| Test | Type | Intentional Defect |
|---|---|---|
| RCA-001 | API | Asserts HTTP 200 but service correctly returns 201 Created |
| RCA-002 | UI | Asserts "Welcome to Grand Hotel" but page shows "Welcome to Shady Meadows B&B" |

Both are **test defects, not system defects** — the SUT behaves correctly. The RCA demo runs separately and does not affect the main suite pass/fail status.

```bash
# Run RCA demo (build succeeds even though tests fail)
./gradlew rcaDemo

# View combined report (passing tests + intentional failures)
./gradlew allureServe
```

### Manual Tests (3 scenarios — step-by-step tutorial with screenshots)

A [Manual Test Tutorial](docs/ManualTestTutorial.md) provides step-by-step instructions for manual testers, with reference screenshots captured by Selenium:

| Scenario | Steps | What It Covers |
|---|---|---|
| TC-UI-001 | 9 | Homepage UI validation (hero, nav, rooms, location, contact, footer) |
| TC-BOOK-001 | 14 | End-to-end room booking flow |
| TC-MSG-001 | 4 | Contact form submission |
| TC-RCA-001/002 | 7+4 | RCA walkthrough using the Allure dashboard |

### CI Pipeline (GitHub Actions)

Every push triggers the full suite automatically. The pipeline:
1. Starts the SUT via Docker Compose
2. Runs all 23 automated tests
3. Runs the RCA demo (intentional failures)
4. Generates and uploads the Allure report as a downloadable artifact

See the [Actions tab](https://github.com/ddreakford/test-automation-demo/actions) for pipeline runs.

## JDK Setup

The test project compiles to Java 17 bytecode but **requires JDK 21 to run Gradle 8.14**. If your default JDK is 21, everything works out of the box. If not, install JDK 21 and either:

- Set `JAVA_HOME` to your JDK 21 installation, or
- Uncomment the appropriate line in `rbp-test-demo/gradle.properties`, or
- Pass it on the command line: `./gradlew clean test -Dorg.gradle.java.home=/path/to/jdk-21`

| Platform | Install | Typical Path |
|---|---|---|
| macOS (Homebrew) | `brew install openjdk@21` | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |
| Linux (apt) | `sudo apt install openjdk-21-jdk` | `/usr/lib/jvm/java-21-openjdk-amd64` |
| Windows | [Adoptium](https://adoptium.net) installer | `C:\Program Files\Eclipse Adoptium\jdk-21` |

## Documentation

| Document | Description |
|---|---|
| [Setup Guide](docs/TestAutomationDemo_SetupGuide.md) | Full walkthrough — architecture, source code, Allure report, RCA demo |
| [Manual Test Tutorial](docs/ManualTestTutorial.md) | Step-by-step manual test scenarios with reference screenshots |

To generate `.docx` versions for sharing:
```bash
./scripts/convert-to-docx.sh
```

## Project Structure

```
test-automation-demo/
├── .github/workflows/         # GitHub Actions CI pipeline
├── docs/
│   ├── TestAutomationDemo_SetupGuide.md   # Setup & walkthrough guide
│   ├── ManualTestTutorial.md              # Manual test scenarios
│   └── screenshots/                       # Reference screenshots
├── scripts/                   # Utility scripts (docx conversion)
├── restful-booker-platform/   # System under test (git submodule)
└── rbp-test-demo/             # Gradle test project
    └── src/test/java/com/demo/tests/
        ├── api/               # API tests (Auth, Booking)
        ├── ui/                # Admin UI test
        ├── regression/        # Homepage, Booking Flow, Contact Form
        ├── rca/               # Intentional failures for RCA demo
        ├── base/              # UIBase, ApiBase
        ├── models/            # Booking POJOs
        └── screenshots/       # Screenshot capture utility
```

## Acknowledgements

The system under test is the **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)** by [Mark Winteringham](https://github.com/mwinteringham). It is an open-source, multi-service hotel booking application built specifically for test automation training. It is included in this repository as a Git submodule — all credit for its design and implementation belongs to Mark Winteringham and contributors to **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)**.

- **Repository:** https://github.com/mwinteringham/restful-booker-platform
- **License:** [GPL-3.0](https://github.com/mwinteringham/restful-booker-platform/blob/master/LICENSE)
- **Author's site:** https://www.mwtestconsultancy.co.uk
