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

- JDK 21 (see [JDK Setup](#jdk-setup) below)
- Docker Desktop (with Docker Compose)
- Google Chrome (latest stable)
- Git

## Quick Start

```bash
# 1. Clone with submodules
git clone --recurse-submodules https://github.com/ddreakford/test-automation-demo.git
cd test-automation-demo

# 2. Start the system under test
cd restful-booker-platform
docker compose up -d
cd ..

# 3. Wait ~20 seconds for services to initialise, then run the test suite
cd rbp-test-demo
./gradlew clean test

# 4. View the Allure report
./gradlew allureServe
```

> **NOTE:** If your system JDK is newer than 21 (e.g., JDK 25), Gradle may fail. See [JDK Setup](#jdk-setup) below.

## JDK Setup

The test project compiles to Java 17 bytecode but **requires JDK 21 to run Gradle 8.14**. If your default JDK is 21, everything works out of the box. If not, install JDK 21 and either:

- Set `JAVA_HOME` to your JDK 21 installation, or
- Uncomment the appropriate line in `rbp-test-demo/gradle.properties`, or
- Pass it on the command line: `./gradlew clean test -Dorg.gradle.java.home=/path/to/jdk-21`

Platform-specific JDK 21 install:

| Platform | Install | Typical Path |
|---|---|---|
| macOS (Homebrew) | `brew install openjdk@21` | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |
| Linux (apt) | `sudo apt install openjdk-21-jdk` | `/usr/lib/jvm/java-21-openjdk-amd64` |
| Windows | [Adoptium](https://adoptium.net) installer | `C:\Program Files\Eclipse Adoptium\jdk-21` |

## Documentation

See [docs/TestAutomationDemo_SetupGuide.md](docs/TestAutomationDemo_SetupGuide.md) for the full walkthrough — explains the architecture, each source file, and how to use the Allure report for root cause analysis.

To generate a `.docx` version for sharing:
```bash
./scripts/convert-to-docx.sh
```

## Project Structure

```
test-automation-demo/
├── CLAUDE.md                  # Project conventions for Claude Code
├── README.md
├── .github/workflows/         # GitHub Actions CI pipeline
├── docs/                      # Setup guide (.md source + .docx for sharing)
├── scripts/                   # Utility scripts
├── restful-booker-platform/   # System under test (git submodule)
└── rbp-test-demo/             # Gradle test project (test automation code)
```

## Acknowledgements

The system under test is the **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)** by [Mark Winteringham](https://github.com/mwinteringham). It is an open-source, multi-service hotel booking application built specifically for test automation training. It is included in this repository as a Git submodule — all credit for its design and implementation belongs to Mark Winteringham and contributors to **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)**.

- **Repository:** https://github.com/mwinteringham/restful-booker-platform
- **License:** [GPL-3.0](https://github.com/mwinteringham/restful-booker-platform/blob/master/LICENSE)
- **Author's site:** https://www.mwtestconsultancy.co.uk
