# Test Automation Demo

## Setup & Execution Guide

*RestAssured • Selenium • TestNG • Gradle • Allure*

|  |  |
|----|----|
| **Purpose** | Step-by-step guide to set up, run, and understand a test automation demo covering API and UI testing across multiple backend services, with a visual results dashboard and root cause analysis walkthrough. |
| **Audience** | Individual setup, team onboarding, or job interview / assessment demonstration |
| **Test Target** | Restful-Booker Platform (Docker Compose) — multi-service hotel booking application |
| **Stack** | Java 17 (source) • JDK 21 (build) • Gradle 8.14 • TestNG • RestAssured • Selenium WebDriver • Allure Report |
| **Tracking** | Use the checkboxes throughout this document to mark steps complete as you go |

> **How to use this guide:** This is a **clone-and-understand** walkthrough. The repository already contains a fully working test project. You will clone it, start the system under test, run the tests, and then walk through each component to understand how it works. If you want to build the project from scratch as a learning exercise, the code listings in each section serve as a reference — but the focus here is on getting up and running quickly and understanding each piece.

---

## Section 1 — Prerequisites & Environment

Complete all items in this section before proceeding. Each tool is required for the demo to run end to end.

### 1.1 Required Tools

Install and verify each of the following:

- [ ] **JDK 21**

  ```bash
  java -version
  # Expected: openjdk 21.x.x
  ```

  > **NOTE:** The test source code targets Java 17, but **JDK 21 is required to run Gradle 8.14**. If your system default JDK is newer (e.g., 25), you can install JDK 21 side-by-side and configure it for this project — see Section 5.2.
  >
  > | Platform | Install | Typical `JAVA_HOME` Path |
  > |---|---|---|
  > | macOS (Homebrew) | `brew install openjdk@21` | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |
  > | Linux (apt) | `sudo apt install openjdk-21-jdk` | `/usr/lib/jvm/java-21-openjdk-amd64` |
  > | Windows | [Adoptium](https://adoptium.net) installer | `C:\Program Files\Eclipse Adoptium\jdk-21` |
  >
  > Set `JAVA_HOME` to your JDK 21 path. For fish shell: `set -Ux JAVA_HOME (/usr/libexec/java_home -v 21)`

- [ ] **Docker Desktop** — running and accepting connections

  ```bash
  docker --version
  docker compose version
  docker ps
  # Expected: no errors; empty container list is fine
  ```

  > **NOTE:** Download from https://www.docker.com/products/docker-desktop. Ensure Docker Desktop is started before running the system under test. Docker Compose is included with Docker Desktop.

- [ ] **Google Chrome** (latest stable)

- [ ] **Git**

### 1.2 Verify Network Ports Are Available

The Restful-Booker Platform runs as multiple Docker containers, each on its own port:

| Port | Service |
|------|---------|
| 80   | Assets / Web UI (Next.js proxy) |
| 3000 | Booking service |
| 3001 | Room service |
| 3002 | Branding service |
| 3004 | Auth service |
| 3005 | Report service |
| 3006 | Message service |

```bash
# macOS / Linux — check all required ports
for port in 80 3000 3001 3002 3004 3005 3006; do
  echo -n "Port $port: "
  lsof -i :$port >/dev/null 2>&1 && echo "IN USE" || echo "free"
done
# Expected: all ports show "free"
```

---

## Section 2 — Clone the Repository

This repository contains both the test automation project and the system under test (SUT). The SUT — [Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform) by Mark Winteringham — is included as a **Git submodule**. You must initialise it when cloning.

### 2.1 Clone with Submodules (recommended)

```bash
git clone --recurse-submodules https://github.com/ddreakford/test-automation-demo.git
cd test-automation-demo
```

- [ ] Repository cloned with submodules

### 2.2 Already Cloned Without Submodules?

If the `restful-booker-platform/` directory is empty, initialise the submodule manually:

```bash
git submodule init
git submodule update
```

### 2.3 Verify the Submodule

```bash
ls restful-booker-platform/docker-compose.yml
# Expected: file exists

git submodule status
# Expected: shows a commit hash followed by "restful-booker-platform"
```

- [ ] `restful-booker-platform/docker-compose.yml` exists

> **NOTE:** The `restful-booker-platform/` directory is a reference to the upstream repository at https://github.com/mwinteringham/restful-booker-platform. All credit for the SUT's design and implementation belongs to Mark Winteringham and its contributors. See [Acknowledgements](#acknowledgements).

---

## Section 3 — Platform Stack Overview (Reference)

This section summarises every tool in the demo and why each was selected. Use this as a reference when explaining your choices during an interview.

| **Layer** | **Tool** | **Purpose** |
|----|----|----|
| System Under Test | Restful-Booker Platform (Docker Compose) | Multi-service hotel booking app with REST APIs + web UI. Free and open source. |
| API Testing | RestAssured + TestNG | Industry-standard Java API testing. Full request/response validation with fluent syntax. |
| UI Testing | Selenium WebDriver + TestNG | Industry-standard browser automation. Integrates cleanly with RestAssured in one project. |
| Build Tool | Gradle (Groovy DSL) | Fast incremental builds. Manages all dependencies and test execution. |
| Reporting | Allure Report | Visual dashboard with pass/fail trends, RCA drill-down, request/response attachments, and screenshots. |
| CI (Optional) | GitHub Actions | Runs the full suite on every push. Demonstrates end-to-end pipeline awareness. |

### 3.1 System Under Test — Restful-Booker Platform

Restful-Booker Platform is a hotel booking application built specifically for test automation training. It runs as a set of Docker containers via Docker Compose and exposes seven independent microservices, each with its own REST API and port:

| Service | Port | Purpose |
|---------|------|---------|
| **auth** | 3004 | Token-based authentication (cookie-based JWT) |
| **booking** | 3000 | Full CRUD for guest reservations |
| **room** | 3001 | Room inventory management |
| **report** | 3005 | Stay summary and reporting |
| **branding** | 3002 | UI configuration and theming |
| **message** | 3006 | Contact / messaging service |
| **assets** | 80 | Next.js web UI with reverse proxy to backend services |

The web UI is served on port 80. For test automation, we call the service APIs directly on their individual ports.

Swagger documentation for each service is available at:

```
http://localhost:{port}/{service}/swagger-ui/index.html
# e.g. http://localhost:3000/booking/swagger-ui/index.html
```

> **TIP:** The multi-service architecture lets you demonstrate cross-service test coverage: an Auth token obtained from the auth service (port 3004) is then used as a cookie credential in booking service tests (port 3000).

### 3.2 Authentication Model

The auth service uses **cookie-based tokens**, not JSON response bodies:

- `POST /auth/login` returns HTTP 200 with a `Set-Cookie: token=<value>` header
- The response body is **empty** (Content-Length: 0)
- Protected endpoints accept the token via `Cookie: token=<value>` header
- Tokens expire after 1 hour

---

## Section 4 — Start the System Under Test

### 4.1 Start via Docker Compose

After cloning the repository with submodules (Section 2), start all services:

```bash
# From the test-automation-demo project root:
cd restful-booker-platform
docker compose up -d
cd ..
```

This pulls all seven service images and starts them. The first run may take a few minutes to download images.

> **NOTE:** The images are built for `linux/amd64`. On Apple Silicon Macs, Docker Desktop runs them under Rosetta emulation automatically. You may see platform mismatch warnings — these are safe to ignore.

- [ ] All containers started via `docker compose up -d`
- [ ] All containers appear healthy in: `docker compose ps`

### 4.2 Verify All Services Are Responding

Wait 15-20 seconds after startup for all Spring Boot services to initialise, then run each check:

**Web UI**

```bash
open http://localhost
# Expected: hotel booking homepage loads in browser
```

- [ ] Web UI loads at http://localhost (port 80)

**Auth Service**

```bash
curl -s -c - -X POST http://localhost:3004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
# Expected: Set-Cookie header with token value (body is empty)
```

- [ ] Auth service returns a token cookie

**Booking Service** (requires auth cookie)

```bash
TOKEN=$(curl -s -c - -X POST http://localhost:3004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' \
  | grep token | awk '{print $NF}')
curl -s -b "token=$TOKEN" http://localhost:3000/booking/
# Expected: JSON with bookings array
```

- [ ] Booking service returns booking list

**Room Service**

```bash
curl -s http://localhost:3001/room/
# Expected: JSON with rooms array
```

- [ ] Room service returns room list

> **IMPORTANT:** If a service does not respond, check Docker Desktop to confirm all containers are running. Spring Boot services initialise sequentially and may take 15-20 seconds.

### 4.3 Stop / Restart the Platform

```bash
# Stop all services (from restful-booker-platform/)
cd restful-booker-platform
docker compose stop

# Start again (images already downloaded)
docker compose start

# Full reset (removes containers and recreates from images)
docker compose down && docker compose up -d
```

---

## Section 5 — Gradle Project Overview

The test project is at `rbp-test-demo/`. It is already fully configured in the repository — you do not need to create any files.

> **Building from scratch?** If you want to recreate this project as a learning exercise, you would run `gradle init --type java-library --dsl groovy` in an empty directory, then add the dependencies and source files described below. That workflow is not covered in this guide.

### 5.1 Verify the Build

```bash
# From the test-automation-demo project root:
cd rbp-test-demo
./gradlew --version
# Expected: Gradle 8.14, JDK 21
```

- [ ] Gradle wrapper runs successfully

### 5.2 JDK Configuration

The Gradle wrapper requires JDK 21. If your system default `JAVA_HOME` points to JDK 21, everything works. If not, you have three options:

1. **Set `JAVA_HOME`** to your JDK 21 installation before running Gradle
2. **Edit `gradle.properties`** — uncomment the line matching your OS
3. **Pass it on the command line:** `./gradlew clean test -Dorg.gradle.java.home=/path/to/jdk-21`

### 5.3 Key Build Files

**`settings.gradle`** — declares the project name:

```groovy
rootProject.name = 'rbp-test-demo'
```

**`build.gradle`** — manages dependencies, the Allure plugin, AspectJ agent wiring, and the TestNG suite runner:

```groovy
plugins {
    id 'java'
    id 'io.qameta.allure' version '2.11.2'
}

group = 'com.demo.tests'
version = '1.0-SNAPSHOT'
sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

ext {
    allureVersion = '2.25.0'
    aspectjVersion = '1.9.21'
}

configurations {
    agent // AspectJ weaver — required by Allure
}

dependencies {
    agent "org.aspectj:aspectjweaver:${aspectjVersion}"
    testImplementation "org.testng:testng:7.9.0"
    testImplementation "io.rest-assured:rest-assured:5.4.0"
    testImplementation "org.seleniumhq.selenium:selenium-java:4.18.1"
    testImplementation "io.github.bonigarcia:webdrivermanager:5.7.0"
    testImplementation "io.qameta.allure:allure-testng:${allureVersion}"
    testImplementation "io.qameta.allure:allure-rest-assured:${allureVersion}"
    testImplementation "com.fasterxml.jackson.core:jackson-databind:2.17.0"
}

test {
    useTestNG {
        suites 'src/test/resources/testng.xml'
    }
    jvmArgs "-javaagent:${configurations.agent.asPath}"
    testLogging {
        events 'passed', 'skipped', 'failed'
    }
}

allure {
    version = allureVersion
    useTestNG {
        version = allureVersion
    }
}
```

> **NOTE:** `sourceCompatibility = JavaVersion.VERSION_17` means the compiled bytecode targets Java 17. The Gradle build tool itself requires JDK 21+ to run. The `agent` configuration wires AspectJ bytecode weaving into the test JVM — without it, Allure `@Step` annotations and request/response attachments will not appear in the report.

---

## Section 6 — Project Structure & Source Files

### 6.1 Directory Layout

```
rbp-test-demo/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew / gradlew.bat
└── src/
    └── test/
        ├── java/com/demo/tests/
        │   ├── api/
        │   │   ├── AuthApiTest.java      # Auth service API tests
        │   │   └── BookingApiTest.java    # Booking CRUD API tests
        │   ├── ui/
        │   │   └── BookingUITest.java     # Browser-based admin login test
        │   ├── base/
        │   │   ├── ApiBase.java           # RestAssured config + service URLs
        │   │   └── UIBase.java            # Chrome setup + screenshot capture
        │   └── models/
        │       ├── Booking.java           # Booking request POJO
        │       └── BookingDates.java      # Date range POJO
        └── resources/
            └── testng.xml                 # Test suite definition
```

### 6.2 testng.xml

Controls test suite execution. `parallel='classes'` runs each test class in its own thread:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="RBP Demo Suite" parallel="classes" thread-count="2">
    <test name="Auth Service Tests">
        <classes>
            <class name="com.demo.tests.api.AuthApiTest"/>
        </classes>
    </test>
    <test name="Booking Service Tests">
        <classes>
            <class name="com.demo.tests.api.BookingApiTest"/>
        </classes>
    </test>
    <test name="Booking UI Tests">
        <classes>
            <class name="com.demo.tests.ui.BookingUITest"/>
        </classes>
    </test>
</suite>
```

---

## Section 7 — Base Classes

### 7.1 ApiBase.java

Configures RestAssured and attaches the AllureRestAssured filter, which automatically captures every HTTP request and response as an attachment in the report.

Because each microservice runs on its own port, `ApiBase` defines per-service URL constants rather than a single `baseURI`:

```java
package com.demo.tests.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.testng.annotations.BeforeSuite;

public class ApiBase {

    protected static final String AUTH_URL = "http://localhost:3004";
    protected static final String BOOKING_URL = "http://localhost:3000";
    protected static final String ROOM_URL = "http://localhost:3001";
    protected static final String BRANDING_URL = "http://localhost:3002";
    protected static final String REPORT_URL = "http://localhost:3005";
    protected static final String MESSAGE_URL = "http://localhost:3006";

    protected static String authToken;

    @BeforeSuite
    public void setupRestAssured() {
        RestAssured.filters(
            new AllureRestAssured(),
            new RequestLoggingFilter(),
            new ResponseLoggingFilter()
        );
    }
}
```

### 7.2 UIBase.java

Launches Chrome before each test and captures a screenshot automatically on any failure. In CI environments (where `CI=true`), Chrome runs in headless mode:

```java
package com.demo.tests.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class UIBase {

    protected WebDriver driver;
    protected static final String UI_URL = "http://localhost";

    @BeforeMethod
    public void setupDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        if ("true".equals(System.getenv("CI"))) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }
        driver = new ChromeDriver(options);
        driver.get(UI_URL);
    }

    @AfterMethod
    public void teardown(ITestResult result) {
        if (!result.isSuccess()) {
            captureScreenshot();
        }
        if (driver != null) driver.quit();
    }

    @Attachment(value = "Failure Screenshot", type = "image/png")
    private byte[] captureScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
```

---

## Section 8 — Model Classes

These POJOs are serialised to JSON by Jackson when RestAssured sends request bodies. They match the Booking Service API contract.

> **NOTE:** The Booking Service API requires a `roomid` field and does not use `totalprice` or `additionalneeds` (those fields exist in the original restful-booker single-service app but not in the platform version).

### 8.1 BookingDates.java

```java
package com.demo.tests.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingDates {

    @JsonProperty("checkin") public String checkin;
    @JsonProperty("checkout") public String checkout;

    public BookingDates(String in, String out) {
        this.checkin = in;
        this.checkout = out;
    }
}
```

### 8.2 Booking.java

```java
package com.demo.tests.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Booking {

    @JsonProperty("roomid") public int roomid;
    @JsonProperty("firstname") public String firstname;
    @JsonProperty("lastname") public String lastname;
    @JsonProperty("depositpaid") public boolean depositpaid;
    @JsonProperty("bookingdates") public BookingDates bookingdates;

    public Booking(int roomid, String fn, String ln, boolean deposit,
                   BookingDates dates) {
        this.roomid = roomid;
        this.firstname = fn;
        this.lastname = ln;
        this.depositpaid = deposit;
        this.bookingdates = dates;
    }
}
```

---

## Section 9 — Test Classes

### 9.1 AuthApiTest.java

Tests the Auth microservice. Validates that valid credentials return a token cookie (HTTP 200 with `Set-Cookie` header) and that invalid credentials are rejected:

```java
package com.demo.tests.api;

import com.demo.tests.base.ApiBase;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

@Epic("Auth Service")
@Feature("Authentication API")
public class AuthApiTest extends ApiBase {

    @Test
    @Story("Valid credentials return a token")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /auth/login with valid creds returns HTTP 200 with a token cookie")
    public void validLoginReturnsToken() {
        String payload = "{\"username\":\"admin\",\"password\":\"password\"}";

        Response response = given()
            .header("Content-Type", "application/json")
            .body(payload)
            .when()
            .post(AUTH_URL + "/auth/login")
            .then()
            .statusCode(200)
            .extract().response();

        String tokenCookie = response.getCookie("token");
        assertNotNull(tokenCookie, "Token cookie should be present");
        assertFalse(tokenCookie.isEmpty(), "Token cookie should not be empty");
        authToken = tokenCookie;
    }

    @Test
    @Story("Invalid credentials are rejected")
    @Severity(SeverityLevel.NORMAL)
    public void invalidLoginReturns403() {
        String payload = "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";

        given()
            .header("Content-Type", "application/json")
            .body(payload)
            .when()
            .post(AUTH_URL + "/auth/login")
            .then()
            .statusCode(403);
    }
}
```

### 9.2 BookingApiTest.java

Tests the Booking microservice with a full CRUD lifecycle: create a booking, retrieve it by ID, then delete it using an auth token cookie. Tests are ordered via `priority` and chained via `dependsOnMethods`. Booking dates are dynamically set 6 months in the future to avoid conflicts with existing data:

```java
package com.demo.tests.api;

import com.demo.tests.base.ApiBase;
import com.demo.tests.models.Booking;
import com.demo.tests.models.BookingDates;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Booking Service")
@Feature("Booking CRUD API")
public class BookingApiTest extends ApiBase {

    private int bookingId;

    @BeforeClass
    public void authenticate() {
        String payload = "{\"username\":\"admin\",\"password\":\"password\"}";

        Response response = given()
            .header("Content-Type", "application/json")
            .body(payload)
            .post(AUTH_URL + "/auth/login")
            .then()
            .extract().response();

        authToken = response.getCookie("token");
    }

    @Test(priority = 1)
    @Story("Create a new booking")
    @Severity(SeverityLevel.CRITICAL)
    public void createBooking() {
        // Use future dates to avoid conflicts with existing bookings
        String checkin = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String checkout = LocalDate.now().plusMonths(6).plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE);

        Booking booking = new Booking(
            1, "James", "Brown", true,
            new BookingDates(checkin, checkout)
        );

        Response response = given()
            .header("Content-Type", "application/json")
            .body(booking)
            .when()
            .post(BOOKING_URL + "/booking/")
            .then()
            .statusCode(201)
            .body("bookingid", notNullValue())
            .extract().response();

        bookingId = response.jsonPath().getInt("bookingid");
    }

    @Test(priority = 2, dependsOnMethods = "createBooking")
    @Story("Retrieve an existing booking")
    @Severity(SeverityLevel.CRITICAL)
    public void getBookingById() {
        given()
            .cookie("token", authToken)
            .when()
            .get(BOOKING_URL + "/booking/" + bookingId)
            .then()
            .statusCode(200)
            .body("firstname", equalTo("James"))
            .body("lastname", equalTo("Brown"));
    }

    @Test(priority = 3, dependsOnMethods = "createBooking")
    @Story("Delete a booking")
    @Severity(SeverityLevel.NORMAL)
    public void deleteBooking() {
        given()
            .cookie("token", authToken)
            .when()
            .delete(BOOKING_URL + "/booking/" + bookingId)
            .then()
            .statusCode(202);
    }
}
```

### 9.3 BookingUITest.java

Tests the admin login flow via the browser. Navigates to the admin panel, submits credentials, and asserts the rooms panel is visible. Any failure automatically attaches a screenshot via UIBase:

```java
package com.demo.tests.ui;

import com.demo.tests.base.UIBase;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

@Epic("Booking UI")
@Feature("Admin Panel")
public class BookingUITest extends UIBase {

    @Test
    @Story("Admin can log in and view room list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Validates admin login flow and confirms rooms panel is visible")
    public void adminLoginAndViewRooms() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get(UI_URL + "/admin");

        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("password");
        driver.findElement(By.id("doLogin")).click();

        WebElement roomsPanel = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='roomlisting']"))
        );

        Assert.assertTrue(roomsPanel.isDisplayed(),
            "Rooms panel should be visible after login");
    }
}
```

---

## Section 10 — Run Tests & Generate the Report

### 10.1 Run the Full Test Suite

```bash
# From rbp-test-demo/ (make sure the SUT is running — Section 4)
./gradlew clean test
# Expected output: test count, pass/skip/fail summary in terminal
```

- [ ] `./gradlew clean test` completes with all 6 tests passing

> **NOTE:** If Gradle reports UP-TO-DATE and skips tests, use `./gradlew clean test` to force a fresh run. If the build fails due to JDK version, see Section 5.2.

### 10.2 Generate and Open the Allure Report

```bash
# Serve live (opens browser automatically — best for demos)
./gradlew allureServe

# Or generate static HTML (useful for saving or CI artifacts)
./gradlew allureReport
# Output: build/reports/allure-report/allureReport/index.html
```

- [ ] Allure report opens in browser
- [ ] All tests visible in the Overview panel

### 10.3 Allure Dashboard Walkthrough

Use this table when presenting the dashboard. Each row is one talking point:

| **Dashboard Panel** | **What It Shows** | **Demo Talking Point** |
|----|----|----|
| Overview | Pass/fail rate broken down by Epic > Feature > Story | Mirrors how a team organises test ownership across services |
| Suites | Each TestNG class and individual test execution time | Useful for spotting slow tests and optimisation opportunities |
| Behaviors | Tests grouped by @Epic / @Feature / @Story annotations | Shows coverage from a PM or QA Lead perspective |
| Timeline | Parallel execution visualised across threads | Demonstrates awareness of test efficiency and concurrency |
| Failed Test Drill-down | Full request/response body, stack trace, failure screenshot | This is the RCA story — see Section 10.4 |

### 10.4 Root Cause Analysis (RCA) Demo Script

This is an **optional, deliberate failure** designed to practice demonstrating RCA skills (e.g., during an interview). Skip this on first run — come back to it once you're comfortable with the green suite.

**Step 1 — Introduce a Known Failure**

In `BookingApiTest.java`, temporarily change the status code assertion in `createBooking()`:

```java
// Before (correct)
.statusCode(201)

// After (intentionally wrong — simulates a misconfigured assertion)
.statusCode(200)
```

**Step 2 — Re-run and Open the Report**

```bash
./gradlew clean test allureServe
```

**Step 3 — Walk Through the RCA in the Report**

Click the failing test in Allure and walk through each layer:

1. **Assertion layer:** "Expected status code 200 but was 201" — the exact line that failed
2. **Request layer:** full HTTP POST body sent to /booking/ — confirm request was correct
3. **Response layer:** HTTP 201 received from service — confirm service behaved correctly
4. **Conclusion:** "This is a test defect, not a service defect. The service returned the correct HTTP 201 Created. The assertion was misconfigured to expect 200. Root cause is in the test, not the system under test."

> **TIP:** This distinction — test defect vs system defect — is exactly what interviewers want to hear.

**Step 4 — Restore the Correct Assertion**

```java
.statusCode(201) // restore
```

```bash
./gradlew clean test allureServe
```

- [ ] RCA walk-through rehearsed

---

## Section 11 — GitHub Actions CI Pipeline

The repository includes a CI pipeline that runs the full test suite on every push. It is already configured — no setup required.

### 11.1 How It Works

The workflow file at `.github/workflows/test.yml`:

1. Checks out the repo with `submodules: recursive`
2. Sets up JDK 21 via `actions/setup-java`
3. Starts the SUT via `docker compose up -d`
4. Polls the health endpoint until services are ready
5. Installs Chrome and runs the test suite (headless via `CI=true`)
6. Generates the Allure report and uploads it as a downloadable artifact

**Key CI-specific details:**
- `CI=true` (set automatically by GitHub Actions) triggers headless Chrome in `UIBase`
- `-Dorg.gradle.java.home=$JAVA_HOME` overrides `gradle.properties` so the runner's JDK is used
- `if: always()` on report/artifact steps ensures they run even when tests fail — essential for RCA

### 11.2 Viewing CI Results

After pushing, go to the **Actions** tab on the GitHub repository to see the pipeline run. The Allure report and HTML test results are uploaded as downloadable artifacts.

> **TIP:** In an interview, pull up the CI run from the GitHub Actions tab to show the full CI-to-report pipeline.

### 11.3 Forking This Repository

If you fork this repo, GitHub Actions will be enabled automatically. The pipeline will run on your first push. No changes needed — the workflow uses relative paths and `JAVA_HOME` from the runner.

---

## Section 12 — Quick Reference

### 12.1 Key Commands

```bash
# Start system under test (from project root)
cd restful-booker-platform && docker compose start && cd ..

# Run tests (from rbp-test-demo/)
cd rbp-test-demo && ./gradlew clean test

# Open Allure report
./gradlew allureServe

# Generate static report
./gradlew allureReport

# Stop system under test
cd restful-booker-platform && docker compose stop && cd ..

# Full reset of SUT
cd restful-booker-platform && docker compose down && docker compose up -d && cd ..
```

### 12.2 Default Credentials

| | |
|----|-----|
| **Admin Username** | admin |
| **Admin Password** | password |
| **UI URL** | http://localhost (port 80) |
| **Auth Endpoint** | http://localhost:3004/auth/login |
| **Booking API** | http://localhost:3000/booking/ |
| **Room API** | http://localhost:3001/room/ |
| **Swagger (each)** | http://localhost:{port}/{service}/swagger-ui/index.html |

### 12.3 Service Port Map

| Service | Port | Direct URL |
|---------|------|------------|
| Assets / Web UI | 80 | http://localhost |
| Booking | 3000 | http://localhost:3000/booking/ |
| Room | 3001 | http://localhost:3001/room/ |
| Branding | 3002 | http://localhost:3002/branding/ |
| Auth | 3004 | http://localhost:3004/auth/ |
| Report | 3005 | http://localhost:3005/report/ |
| Message | 3006 | http://localhost:3006/message/ |

### 12.4 Progress Tracker

- [ ] Section 1 — Prerequisites installed and verified
- [ ] Section 2 — Repository cloned with submodules
- [ ] Section 4 — Docker containers running, all services responding
- [ ] Section 5 — Gradle build verified
- [ ] Section 10 — Tests run green, Allure report opens
- [ ] Section 10 — (Optional) RCA demo rehearsed
- [ ] Section 11 — CI pipeline runs green

---

## Acknowledgements

The system under test is the **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)** by [Mark Winteringham](https://github.com/mwinteringham). It is an open-source, multi-service hotel booking application built specifically for test automation training. It is included in this repository as a Git submodule — all credit for its design and implementation belongs to Mark Winteringham and its contributors.

- **Repository:** https://github.com/mwinteringham/restful-booker-platform
- **License:** [GPL-3.0](https://github.com/mwinteringham/restful-booker-platform/blob/master/LICENSE)
- **Author's site:** https://www.mwtestconsultancy.co.uk

**Good luck with the interview!**
