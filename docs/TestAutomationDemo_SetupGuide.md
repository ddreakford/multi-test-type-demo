# Test Automation Demo

## Setup & Execution Guide

*RestAssured • Selenium • TestNG • Gradle • Allure*

|  |  |
|----|----|
| **Purpose** | Step-by-step guide to build and run a test automation demo covering API and UI testing across multiple backend services, with a visual results dashboard and root cause analysis walkthrough. |
| **Audience** | Individual setup, team onboarding, or job interview / assessment demonstration |
| **Test Target** | Restful-Booker Platform (Docker Compose) — multi-service hotel booking application |
| **Stack** | Java 17 • Gradle • TestNG • RestAssured • Selenium WebDriver • Allure Report |
| **Tracking** | Use the checkboxes throughout this document to mark steps complete as you go |

---

## Section 1 — Prerequisites & Environment

Complete all items in this section before starting Section 2. Each tool is required for the demo to run end to end.

### 1.1 Required Tools

Install and verify each of the following. Version numbers are minimums — newer versions are fine unless otherwise noted.

- [ ] **Java Development Kit (JDK) 17 or higher**

  ```bash
  java -version
  # Expected: openjdk 17.x.x or higher
  ```

  > **NOTE:** Download from https://adoptium.net if not already installed. Set the `JAVA_HOME` environment variable.
  >
  > For fish shell: `set -Ux JAVA_HOME (/usr/libexec/java_home)`

- [ ] **Gradle 8.x** (or use the Gradle Wrapper — preferred)

  ```bash
  gradle -version
  # Expected: Gradle 8.x
  ```

  > **TIP:** The project includes a Gradle Wrapper (`gradlew`). Once set up you do NOT need a global Gradle install — the wrapper downloads the correct version automatically.

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

The Restful-Booker Platform runs as multiple Docker containers, each on its own port. Confirm nothing else is listening on these ports:

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

If you are cloning for the first time, use `--recurse-submodules` to pull everything in one step:

```bash
git clone --recurse-submodules <repo-url>
cd test-automation-demo
```

- [ ] Repository cloned with submodules

### 2.2 Already Cloned Without Submodules?

If you already cloned the repo but the `restful-booker-platform/` directory is empty, initialise the submodule manually:

```bash
cd test-automation-demo
git submodule init
git submodule update
```

- [ ] Submodule initialised and populated

### 2.3 Verify the Submodule

Confirm the SUT source is present:

```bash
ls restful-booker-platform/docker-compose.yml
# Expected: file exists

git submodule status
# Expected: shows a commit hash followed by "restful-booker-platform"
```

- [ ] `restful-booker-platform/docker-compose.yml` exists

> **NOTE:** The `restful-booker-platform/` directory is a reference to the upstream repository at https://github.com/mwinteringham/restful-booker-platform. All credit for the SUT's design and implementation belongs to Mark Winteringham and its contributors. See the [Acknowledgements](#acknowledgements) section at the end of this guide.

---

## Section 3 — Platform Stack Overview (Reference)

This section summarises every tool in the demo and why each was selected. Use this as a reference when explaining your choices during the interview.

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

The web UI is served on port 80 and proxies API requests to the backend services internally. For test automation, we call the service APIs directly on their individual ports.

Swagger documentation for each service is available at:

```
http://localhost:{port}/{service}/swagger-ui/index.html
# e.g. http://localhost:3000/booking/swagger-ui/index.html
```

> **TIP:** The multi-service architecture lets you demonstrate cross-service test coverage, which is a strong talking point: show that an Auth token obtained from the auth service (port 3004) is then used as a cookie credential in booking service tests (port 3000).

### 3.2 Authentication Model

The auth service uses **cookie-based tokens**, not JSON response bodies:

- `POST /auth/login` returns HTTP 200 with a `Set-Cookie: token=<value>` header
- The response body is **empty** (Content-Length: 0)
- Protected endpoints accept the token via `Cookie: token=<value>` header
- Tokens expire after 1 hour

---

## Section 4 — Start the System Under Test

### 4.1 Start via Docker Compose

After cloning the repository with submodules (Section 2), the `restful-booker-platform/` directory contains the SUT's Docker Compose configuration. Start all services:

```bash
# From the test-automation-demo project root:
cd restful-booker-platform
docker compose up -d
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
# Stop all services
cd restful-booker-platform
docker compose stop

# Start again (images already downloaded)
docker compose start

# Full reset (removes containers and recreates from images)
docker compose down
docker compose up -d
```

---

## Section 5 — Gradle Project Setup

### 5.1 Create Project Directory

```bash
# From the test-automation-demo project root:
mkdir rbp-test-demo
cd rbp-test-demo
```

- [ ] Project directory created

### 5.2 Initialise Gradle Wrapper

Run this to generate the Gradle wrapper files. This makes the project self-contained — no local Gradle install needed:

```bash
gradle init --type java-library --dsl groovy --project-name rbp-test-demo --package com.demo.tests --no-comments --no-split-project
```

> **IMPORTANT:** Gradle 8.14+ is required for JDK 21+ compatibility. If you have a newer JDK (e.g., JDK 25) as your system default, you must also install JDK 21 and configure it for this project. Create a `gradle.properties` file in the project root:
>
> ```properties
> org.gradle.java.home=/path/to/your/jdk-21
> ```
>
> On macOS with Homebrew: `brew install openjdk@21` and use `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

After init, edit `gradle/wrapper/gradle-wrapper.properties` and ensure the distribution URL points to Gradle 8.14:

```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
```

- [ ] Gradle wrapper generated (`gradlew` file exists in project root)

### 5.3 settings.gradle

Ensure the root settings file contains exactly:

```groovy
rootProject.name = 'rbp-test-demo'
```

- [ ] settings.gradle in place

### 5.4 build.gradle

Replace the generated `build.gradle` with the full configuration below. This single file handles all dependencies, the Allure plugin, AspectJ agent wiring, and the TestNG suite runner:

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

- [ ] build.gradle in place

> **IMPORTANT:** The `agent` configuration wires AspectJ bytecode weaving into the test JVM. Without this, Allure will run tests but `@Step` annotations and request/response attachments will not appear in the report.

---

## Section 6 — Project Structure & Source Files

### 6.1 Directory Layout

Create this directory structure under the project root. Each folder and its purpose is described below:

```
rbp-test-demo/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── src/
    └── test/
        ├── java/com/demo/tests/
        │   ├── api/
        │   │   ├── AuthApiTest.java
        │   │   └── BookingApiTest.java
        │   ├── ui/
        │   │   └── BookingUITest.java
        │   ├── base/
        │   │   ├── ApiBase.java
        │   │   └── UIBase.java
        │   └── models/
        │       ├── Booking.java
        │       └── BookingDates.java
        └── resources/
            └── testng.xml
```

- [ ] All directories created

### 6.2 testng.xml (src/test/resources/)

This file controls test suite execution. The `parallel='classes'` setting runs each test class in its own thread:

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

- [ ] testng.xml created

---

## Section 7 — Base Classes

### 7.1 ApiBase.java (base/)

Configures RestAssured and attaches the AllureRestAssured filter, which automatically captures every HTTP request and response as an attachment in the report.

Because each microservice runs on its own port, `ApiBase` does **not** set a global `baseURI`. Instead, test classes specify the full URL for each service they target.

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

- [ ] ApiBase.java created

### 7.2 UIBase.java (base/)

Launches Chrome before each test and captures a screenshot automatically on any failure. The screenshot is attached directly to the Allure report entry for that test:

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

- [ ] UIBase.java created

---

## Section 8 — Model Classes

These POJOs are serialised to JSON by Jackson when RestAssured sends request bodies. They match the Booking Service API contract.

> **NOTE:** The Booking Service API requires a `roomid` field and does not use `totalprice` or `additionalneeds` (those fields exist in the original restful-booker single-service app but not in the platform version).

### 8.1 BookingDates.java (models/)

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

- [ ] BookingDates.java created

### 8.2 Booking.java (models/)

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

- [ ] Booking.java created

---

## Section 9 — Test Classes

### 9.1 AuthApiTest.java (api/)

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

- [ ] AuthApiTest.java created

### 9.2 BookingApiTest.java (api/)

Tests the Booking microservice with a full CRUD lifecycle: create a booking, retrieve it by ID, then delete it using an auth token cookie. Tests are ordered via `priority` and chained via `dependsOnMethods`:

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

- [ ] BookingApiTest.java created

### 9.3 BookingUITest.java (ui/)

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

- [ ] BookingUITest.java created

---

## Section 10 — Run Tests & Generate the Report

### 10.1 Run the Full Test Suite

```bash
# Full clean run (recommended each time for fresh results)
./gradlew clean test
# Expected output: test count, pass/skip/fail summary in terminal
```

- [ ] `./gradlew clean test` completes without build errors

> **NOTE:** If Gradle reports UP-TO-DATE and skips tests, always use `./gradlew clean test` to force a fresh run.

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
| Failed Test Drill-down | Full request/response body, stack trace, failure screenshot | This is the RCA story — see Section 10.4 for the full script |

### 10.4 Root Cause Analysis (RCA) Demo Script

This is a deliberate, controlled failure designed to demonstrate RCA. Walk through these steps during the interview:

**Step 1 — Introduce a Known Failure**

In `BookingApiTest.java`, change the status code assertion in `createBooking()`:

```java
// Before (correct)
.statusCode(201)

// After (intentionally wrong — simulates a misconfigured assertion)
.statusCode(200)
```

- [ ] Intentional failure introduced

**Step 2 — Re-run and Open the Report**

```bash
./gradlew clean test allureServe
```

- [ ] Failure appears in report

**Step 3 — RCA Talking Points in the Report**

Click the failing test in Allure and walk through each layer:

1. **Assertion layer:** "Expected status code 200 but was 201" — the exact line that failed
2. **Request layer:** full HTTP POST body sent to /booking/ — confirm request was correct
3. **Response layer:** HTTP 201 received from service — confirm service behaved correctly
4. **Conclusion:** "This is a test defect, not a service defect. The service returned the correct HTTP 201 Created. The assertion was misconfigured to expect 200. Root cause is in the test, not the system under test."

> **TIP:** This distinction — test defect vs system defect — is exactly what interviewers want to hear. It shows you understand the difference between a failing test and a broken system.

- [ ] RCA walk-through rehearsed

**Step 4 — Restore the Correct Assertion**

```java
.statusCode(201) // restore
```

```bash
./gradlew clean test allureServe
```

- [ ] All tests green again

---

## Section 11 — Optional: GitHub Actions CI Pipeline

Adding CI demonstrates end-to-end pipeline awareness, which is a strong differentiator.

### 11.1 Headless Browser Support

The UI test (`BookingUITest`) runs Chrome in a visible browser window by default. In CI, there is no display, so `UIBase.java` detects the `CI` environment variable (set automatically by GitHub Actions) and switches to headless mode:

```java
if ("true".equals(System.getenv("CI"))) {
    options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
}
```

This is already implemented in the codebase — no manual changes needed.

### 11.2 .github/workflows/test.yml

Create this file at `.github/workflows/test.yml`:

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Grant Gradle wrapper permissions
        run: chmod +x rbp-test-demo/gradlew

      - name: Start Restful-Booker Platform
        run: |
          cd restful-booker-platform
          docker compose up -d

      - name: Wait for services to be ready
        run: |
          echo "Waiting for services to initialise..."
          for i in $(seq 1 30); do
            if curl -sf http://localhost:3004/auth/actuator/health > /dev/null 2>&1; then
              echo "Services are ready (after ${i}s)"
              break
            fi
            sleep 1
          done

      - name: Verify services are responding
        run: |
          curl -sf http://localhost:3001/room/ | head -c 100
          echo
          curl -sf -X POST http://localhost:3004/auth/login \
            -H "Content-Type: application/json" \
            -d '{"username":"admin","password":"password"}' \
            -o /dev/null -w "Auth: HTTP %{http_code}\n"

      - name: Install Chrome
        uses: browser-actions/setup-chrome@v1
        with:
          chrome-version: stable

      - name: Run test suite
        run: |
          cd rbp-test-demo
          ./gradlew clean test

      - name: Generate Allure report
        if: always()
        run: |
          cd rbp-test-demo
          ./gradlew allureReport

      - name: Upload Allure report as artifact
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-report
          path: rbp-test-demo/build/reports/allure-report/allureReport

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: rbp-test-demo/build/reports/tests/test
```

> **Key differences from local setup:**
> - `submodules: recursive` in checkout to pull the SUT
> - JDK 21 via `setup-java` (no need for `gradle.properties` override — JDK 21 is the runner default)
> - Health-check polling loop instead of a fixed `sleep` — more reliable across different runner speeds
> - Service verification step to fail fast if the SUT didn't start
> - Chrome installed via `browser-actions/setup-chrome` for Selenium UI tests
> - `CI=true` is set automatically by GitHub Actions, triggering headless Chrome in `UIBase`
> - `if: always()` on report/artifact steps so they run even when tests fail — essential for RCA

### 11.3 Create GitHub Repository and Push

```bash
# Create the remote repo (requires gh CLI: brew install gh)
gh repo create test-automation-demo --public --source=. --push

# Or if you prefer to create the repo manually on GitHub:
git remote add origin https://github.com/<your-username>/test-automation-demo.git
git push -u origin main
```

- [ ] GitHub repository created and code pushed
- [ ] Actions workflow file committed to `.github/workflows/`
- [ ] Pipeline runs green on first push

> **TIP:** The Allure report and HTML test results are uploaded as downloadable artifacts from the Actions run. In the interview, pull them up from the GitHub Actions tab to show the full CI-to-report pipeline.

---

## Section 12 — Quick Reference

### 12.1 Key Commands

```bash
# Start system under test
cd restful-booker-platform && docker compose start

# Run tests (from rbp-test-demo/)
cd rbp-test-demo && ./gradlew clean test

# Open Allure report
./gradlew allureServe

# Generate static report
./gradlew allureReport

# Stop system under test
cd restful-booker-platform && docker compose stop

# Full reset of SUT
cd restful-booker-platform && docker compose down && docker compose up -d
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

Use this summary checklist for a quick status view:

- [ ] Section 1 — Prerequisites installed and verified
- [ ] Section 2 — Repository cloned with submodules
- [ ] Section 4 — Docker containers running, all services responding
- [ ] Section 5 — Gradle project initialised with build.gradle
- [ ] Section 6 — All source directories and testng.xml created
- [ ] Section 7 — ApiBase and UIBase in place
- [ ] Section 8 — Model classes created
- [ ] Section 9 — All three test classes created
- [ ] Section 10 — Tests run green, Allure report opens
- [ ] Section 10 — RCA demo rehearsed
- [ ] Section 11 — (Optional) CI pipeline runs clean

---

## Acknowledgements

The system under test is the **[Restful-Booker Platform](https://github.com/mwinteringham/restful-booker-platform)** by [Mark Winteringham](https://github.com/mwinteringham). It is an open-source, multi-service hotel booking application built specifically for test automation training. It is included in this repository as a Git submodule — all credit for its design and implementation belongs to Mark Winteringham and its contributors.

- **Repository:** https://github.com/mwinteringham/restful-booker-platform
- **License:** [GPL-3.0](https://github.com/mwinteringham/restful-booker-platform/blob/master/LICENSE)
- **Author's site:** https://www.mwtestconsultancy.co.uk

**Good luck with the interview!**
