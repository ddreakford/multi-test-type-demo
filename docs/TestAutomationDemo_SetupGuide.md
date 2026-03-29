**Test Automation Demo**

Setup & Execution Guide

*RestAssured • Selenium • TestNG • Gradle • Allure*

|  |  |
|----|----|
| **Purpose** | Step-by-step guide to build and run a test automation demo covering API and UI testing across multiple backend services, with a visual results dashboard and root cause analysis walkthrough. |
| **Audience** | Individual setup, team onboarding, or job interview / assessment demonstration |
| **Test Target** | Restful-Booker Platform (Docker) — multi-service hotel booking application |
| **Stack** | Java 17 • Gradle • TestNG • RestAssured • Selenium WebDriver • Allure Report |
| **Tracking** | Use the ☐ checkboxes throughout this document to mark steps complete as you go |

> **Section 1 — Prerequisites & Environment**

Complete all items in this section before starting Phase 2. Each tool is
required for the demo to run end to end.

**1.1 Required Tools**

Install and verify each of the following. Version numbers are minimums —
newer versions are fine unless otherwise noted.

- [ ] Java Development Kit (JDK) 17 or higher

> java -version
>
> \# Expected: openjdk 17.x.x or higher
>
> **NOTE:** *Download from https://adoptium.net if not already
> installed. Set JAVA_HOME environment variable.*

- [ ] Gradle 8.x (or use the Gradle Wrapper — preferred)

> gradle -version
>
> \# Expected: Gradle 8.x
>
> **TIP:** The project includes a Gradle Wrapper (gradlew). Once set up
> you do NOT need a global Gradle install — the wrapper downloads the
> correct version automatically.

- [ ] Docker Desktop — running and accepting connections

> docker --version
>
> docker ps
>
> \# Expected: no errors; empty container list is fine
>
> **NOTE:** *Download from
> https://www.docker.com/products/docker-desktop. Ensure Docker Desktop
> is started before running the system under test.*

- [ ] Google Chrome (latest stable)

- [ ] Git (for optional GitHub Actions CI step)

**1.2 Verify Network Ports Are Available**

The Restful-Booker Platform uses the following port. Confirm nothing
else is listening on it:

> \# macOS / Linux
>
> lsof -i :3003
>
> \# Windows (PowerShell)
>
> netstat -ano \| findstr :3003
>
> \# Expected: no output (port is free)
>
> **Section 2 — Platform Stack Overview**

This section summarises every tool in the demo and why each was
selected. Use this as a reference when explaining your choices during
the interview.

| **Layer** | **Tool** | **Purpose** |
|----|----|----|
| System Under Test | Restful-Booker Platform (Docker) | Multi-service hotel booking app with REST APIs + web UI. Free and open source. |
| API Testing | RestAssured + TestNG | Industry-standard Java API testing. Full request/response validation with fluent syntax. |
| UI Testing | Selenium WebDriver + TestNG | Industry-standard browser automation. Integrates cleanly with RestAssured in one project. |
| Build Tool | Gradle (Groovy DSL) | Fast incremental builds. Manages all dependencies and test execution. |
| Reporting | Allure Report | Visual dashboard with pass/fail trends, RCA drill-down, request/response attachments, and screenshots. |
| CI (Optional) | GitHub Actions | Runs the full suite on every push. Demonstrates end-to-end pipeline awareness. |

**2.1 System Under Test — Restful-Booker Platform**

Restful-Booker Platform is a hotel booking application built
specifically for test automation training. It ships as a Docker image
and exposes five independent microservices, each with its own REST API:

- auth — token-based authentication service

- booking — full CRUD for guest reservations

- room — room inventory management

- report — stay summary and reporting

- branding — UI configuration and theming

All services are proxied through a single port (3003). Swagger
documentation for each service is available at:

> http://localhost:3003/{service}/swagger-ui.html
>
> \# e.g. http://localhost:3003/booking/swagger-ui.html
>
> **TIP:** The multi-service architecture lets you demonstrate
> cross-service test coverage, which is a strong talking point: show
> that an Auth token obtained from the auth service is then used as a
> credential in booking service tests.
>
> **Section 3 — Start the System Under Test**

**3.1 Pull and Run the Docker Image**

Run this command to start all five services and the web UI
simultaneously:

> docker run -d \\
>
> --name rbp \\
>
> -p 3003:3003 \\
>
> mwinteringham/restfulbooker-platform:latest

- [ ] Docker image pulled and container started

- [ ] Container appears in: docker ps

**3.2 Verify All Services Are Responding**

Run each of these checks and confirm the expected response before
continuing:

**Web UI**

> open http://localhost:3003
>
> \# Expected: hotel booking homepage loads in browser

- [ ] Web UI loads at http://localhost:3003

**Auth Service**

> curl -X POST http://localhost:3003/auth/login \\
>
> -H "Content-Type: application/json" \\
>
> -d '{"username":"admin","password":"password"}'
>
> \# Expected: {"token":"\<some_token_value\>"}

- [ ] Auth service returns a token

**Booking Service**

> curl http://localhost:3003/booking/
>
> \# Expected: JSON array of booking IDs, e.g.
> \[{"bookingid":1},{"bookingid":2},...\]

- [ ] Booking service returns booking list

> **IMPORTANT:** If a service does not respond, check Docker Desktop to
> confirm the container is running. Wait 10-15 seconds after startup
> before testing — services initialise sequentially.

**3.3 Stop / Restart the Container**

> \# Stop
>
> docker stop rbp
>
> \# Start again (image already downloaded)
>
> docker start rbp
>
> \# Full reset (removes container, re-creates from image)
>
> docker rm -f rbp && docker run -d --name rbp -p 3003:3003
> mwinteringham/restfulbooker-platform:latest
>
> **Section 4 — Gradle Project Setup**

**4.1 Create Project Directory**

> mkdir rbp-test-demo
>
> cd rbp-test-demo

- [ ] Project directory created

**4.2 Initialise Gradle Wrapper**

Run this to generate the Gradle wrapper files. This makes the project
self-contained — no local Gradle install needed:

> gradle init --type java-library --dsl groovy
>
> \# When prompted:
>
> \# Project name: rbp-test-demo
>
> \# Source package: com.demo.tests
>
> \# Then generate the wrapper at the target version:
>
> gradle wrapper --gradle-version 8.7

- [ ] Gradle wrapper generated (gradlew file exists in project root)

**4.3 settings.gradle**

Ensure the root settings file contains exactly:

> rootProject.name = 'rbp-test-demo'

- [ ] settings.gradle in place

**4.4 build.gradle**

Replace the generated build.gradle with the full configuration below.
This single file handles all dependencies, the Allure plugin, AspectJ
agent wiring, and the TestNG suite runner:

> plugins {
>
> id 'java'
>
> id 'io.qameta.allure' version '2.11.2'
>
> }
>
> group = 'com.demo.tests'
>
> version = '1.0-SNAPSHOT'
>
> sourceCompatibility = JavaVersion.VERSION_17
>
> repositories {
>
> mavenCentral()
>
> }
>
> ext {
>
> allureVersion = '2.25.0'
>
> aspectjVersion = '1.9.21'
>
> }
>
> configurations {
>
> agent // AspectJ weaver — required by Allure
>
> }
>
> dependencies {
>
> agent "org.aspectj:aspectjweaver:\${aspectjVersion}"
>
> testImplementation "org.testng:testng:7.9.0"
>
> testImplementation "io.rest-assured:rest-assured:5.4.0"
>
> testImplementation "org.seleniumhq.selenium:selenium-java:4.18.1"
>
> testImplementation "io.github.bonigarcia:webdrivermanager:5.7.0"
>
> testImplementation "io.qameta.allure:allure-testng:\${allureVersion}"
>
> testImplementation
> "io.qameta.allure:allure-rest-assured:\${allureVersion}"
>
> testImplementation
> "com.fasterxml.jackson.core:jackson-databind:2.17.0"
>
> }
>
> test {
>
> useTestNG {
>
> suites 'src/test/resources/testng.xml'
>
> }
>
> jvmArgs "-javaagent:\${configurations.agent.asPath}"
>
> testLogging {
>
> events 'passed', 'skipped', 'failed'
>
> }
>
> }
>
> allure {
>
> version = allureVersion
>
> useTestNG {
>
> version = allureVersion
>
> }
>
> }

- [ ] build.gradle in place

> **IMPORTANT:** The 'agent' configuration wires AspectJ bytecode
> weaving into the test JVM. Without this, Allure will run tests but
> @Step annotations and request/response attachments will not appear in
> the report.
>
> **Section 5 — Project Structure & Source Files**

**5.1 Directory Layout**

Create this directory structure under the project root. Each folder and
its purpose is described below:

> rbp-test-demo/
>
> ├── build.gradle
>
> ├── settings.gradle
>
> ├── gradlew
>
> ├── gradlew.bat
>
> └── src/
>
> └── test/
>
> ├── java/com/demo/tests/
>
> │ ├── api/
>
> │ │ ├── AuthApiTest.java
>
> │ │ └── BookingApiTest.java
>
> │ ├── ui/
>
> │ │ └── BookingUITest.java
>
> │ ├── base/
>
> │ │ ├── ApiBase.java
>
> │ │ └── UIBase.java
>
> │ └── models/
>
> │ ├── Booking.java
>
> │ └── BookingDates.java
>
> └── resources/
>
> └── testng.xml

- [ ] All directories created

**5.2 testng.xml (src/test/resources/)**

This file controls test suite execution. The parallel='classes' setting
runs each test class in its own thread:

> \<?xml version="1.0" encoding="UTF-8"?\>
>
> \<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd"\>
>
> \<suite name="RBP Demo Suite" parallel="classes" thread-count="2"\>
>
> \<test name="Auth Service Tests"\>
>
> \<classes\>
>
> \<class name="com.demo.tests.api.AuthApiTest"/\>
>
> \</classes\>
>
> \</test\>
>
> \<test name="Booking Service Tests"\>
>
> \<classes\>
>
> \<class name="com.demo.tests.api.BookingApiTest"/\>
>
> \</classes\>
>
> \</test\>
>
> \<test name="Booking UI Tests"\>
>
> \<classes\>
>
> \<class name="com.demo.tests.ui.BookingUITest"/\>
>
> \</classes\>
>
> \</test\>
>
> \</suite\>

- [ ] testng.xml created

> **Section 6 — Base Classes**

**6.1 ApiBase.java (base/)**

Configures RestAssured base URI and attaches the AllureRestAssured
filter, which automatically captures every HTTP request and response as
an attachment in the report:

> package com.demo.tests.base;
>
> import io.qameta.allure.restassured.AllureRestAssured;
>
> import io.restassured.RestAssured;
>
> import io.restassured.filter.log.RequestLoggingFilter;
>
> import io.restassured.filter.log.ResponseLoggingFilter;
>
> import org.testng.annotations.BeforeSuite;
>
> public class ApiBase {
>
> protected static final String BASE_URL = "http://localhost:3003";
>
> protected static String authToken;
>
> @BeforeSuite
>
> public void setupRestAssured() {
>
> RestAssured.baseURI = BASE_URL;
>
> RestAssured.filters(
>
> new AllureRestAssured(),
>
> new RequestLoggingFilter(),
>
> new ResponseLoggingFilter()
>
> );
>
> }
>
> }

- [ ] ApiBase.java created

**6.2 UIBase.java (base/)**

Launches Chrome before each test and captures a screenshot automatically
on any failure. The screenshot is attached directly to the Allure report
entry for that test:

> package com.demo.tests.base;
>
> import io.github.bonigarcia.wdm.WebDriverManager;
>
> import io.qameta.allure.Attachment;
>
> import org.openqa.selenium.OutputType;
>
> import org.openqa.selenium.TakesScreenshot;
>
> import org.openqa.selenium.WebDriver;
>
> import org.openqa.selenium.chrome.ChromeDriver;
>
> import org.openqa.selenium.chrome.ChromeOptions;
>
> import org.testng.ITestResult;
>
> import org.testng.annotations.AfterMethod;
>
> import org.testng.annotations.BeforeMethod;
>
> public class UIBase {
>
> protected WebDriver driver;
>
> protected static final String UI_URL = "http://localhost:3003";
>
> @BeforeMethod
>
> public void setupDriver() {
>
> WebDriverManager.chromedriver().setup();
>
> ChromeOptions options = new ChromeOptions();
>
> options.addArguments("--window-size=1920,1080");
>
> driver = new ChromeDriver(options);
>
> driver.get(UI_URL);
>
> }
>
> @AfterMethod
>
> public void teardown(ITestResult result) {
>
> if (!result.isSuccess()) {
>
> captureScreenshot();
>
> }
>
> if (driver != null) driver.quit();
>
> }
>
> @Attachment(value = "Failure Screenshot", type = "image/png")
>
> private byte\[\] captureScreenshot() {
>
> return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
>
> }
>
> }

- [ ] UIBase.java created

> **Section 7 — Model Classes**

These POJOs are serialised to JSON by Jackson when RestAssured sends
request bodies. They match the Booking Service API contract exactly.

**7.1 BookingDates.java (models/)**

> package com.demo.tests.models;
>
> import com.fasterxml.jackson.annotation.JsonProperty;
>
> public class BookingDates {
>
> @JsonProperty("checkin") public String checkin;
>
> @JsonProperty("checkout") public String checkout;
>
> public BookingDates(String in, String out) {
>
> this.checkin = in;
>
> this.checkout = out;
>
> }
>
> }

- [ ] BookingDates.java created

**7.2 Booking.java (models/)**

> package com.demo.tests.models;
>
> import com.fasterxml.jackson.annotation.JsonProperty;
>
> public class Booking {
>
> @JsonProperty("firstname") public String firstname;
>
> @JsonProperty("lastname") public String lastname;
>
> @JsonProperty("totalprice") public int totalprice;
>
> @JsonProperty("depositpaid") public boolean depositpaid;
>
> @JsonProperty("bookingdates") public BookingDates bookingdates;
>
> @JsonProperty("additionalneeds") public String additionalneeds;
>
> public Booking(String fn, String ln, int price, boolean deposit,
>
> BookingDates dates, String extras) {
>
> this.firstname = fn;
>
> this.lastname = ln;
>
> this.totalprice = price;
>
> this.depositpaid = deposit;
>
> this.bookingdates = dates;
>
> this.additionalneeds = extras;
>
> }
>
> }

- [ ] Booking.java created

> **Section 8 — Test Classes**

**8.1 AuthApiTest.java (api/)**

Tests the Auth microservice. Validates that valid credentials return a
non-empty token and that invalid credentials are rejected with HTTP 403:

> package com.demo.tests.api;
>
> import com.demo.tests.base.ApiBase;
>
> import io.qameta.allure.\*;
>
> import io.restassured.response.Response;
>
> import org.testng.annotations.Test;
>
> import static io.restassured.RestAssured.given;
>
> import static org.hamcrest.Matchers.\*;
>
> @Epic("Auth Service")
>
> @Feature("Authentication API")
>
> public class AuthApiTest extends ApiBase {
>
> @Test
>
> @Story("Valid credentials return a token")
>
> @Severity(SeverityLevel.CRITICAL)
>
> @Description("POST /auth/login with valid creds returns a non-empty
> token")
>
> public void validLoginReturnsToken() {
>
> String payload = "{\\username\\:\\admin\\,\\password\\:\\password\\}";
>
> Response response = given()
>
> .header("Content-Type", "application/json")
>
> .body(payload)
>
> .when()
>
> .post("/auth/login")
>
> .then()
>
> .statusCode(200)
>
> .body("token", not(emptyOrNullString()))
>
> .extract().response();
>
> authToken = response.jsonPath().getString("token");
>
> }
>
> @Test
>
> @Story("Invalid credentials are rejected")
>
> @Severity(SeverityLevel.NORMAL)
>
> public void invalidLoginReturns403() {
>
> String payload =
> "{\\username\\:\\admin\\,\\password\\:\\wrongpassword\\}";
>
> given()
>
> .header("Content-Type", "application/json")
>
> .body(payload)
>
> .when()
>
> .post("/auth/login")
>
> .then()
>
> .statusCode(403);
>
> }
>
> }

- [ ] AuthApiTest.java created

**8.2 BookingApiTest.java (api/)**

Tests the Booking microservice with a full CRUD lifecycle: create a
booking, retrieve it by ID, then delete it using an auth token. Tests
are ordered and chained via dependsOnMethods:

> package com.demo.tests.api;
>
> import com.demo.tests.base.ApiBase;
>
> import com.demo.tests.models.Booking;
>
> import com.demo.tests.models.BookingDates;
>
> import io.qameta.allure.\*;
>
> import io.restassured.response.Response;
>
> import org.testng.annotations.BeforeClass;
>
> import org.testng.annotations.Test;
>
> import static io.restassured.RestAssured.given;
>
> import static org.hamcrest.Matchers.\*;
>
> @Epic("Booking Service")
>
> @Feature("Booking CRUD API")
>
> public class BookingApiTest extends ApiBase {
>
> private int bookingId;
>
> @BeforeClass
>
> public void authenticate() {
>
> String payload = "{\\username\\:\\admin\\,\\password\\:\\password\\}";
>
> authToken = given()
>
> .header("Content-Type", "application/json")
>
> .body(payload)
>
> .post("/auth/login")
>
> .jsonPath().getString("token");
>
> }
>
> @Test(priority = 1)
>
> @Story("Create a new booking")
>
> @Severity(SeverityLevel.CRITICAL)
>
> public void createBooking() {
>
> Booking booking = new Booking(
>
> "James", "Brown", 200, true,
>
> new BookingDates("2024-06-01", "2024-06-05"),
>
> "Breakfast"
>
> );
>
> Response response = given()
>
> .header("Content-Type", "application/json")
>
> .body(booking)
>
> .when()
>
> .post("/booking/")
>
> .then()
>
> .statusCode(201)
>
> .body("bookingid", notNullValue())
>
> .extract().response();
>
> bookingId = response.jsonPath().getInt("bookingid");
>
> }
>
> @Test(priority = 2, dependsOnMethods = "createBooking")
>
> @Story("Retrieve an existing booking")
>
> @Severity(SeverityLevel.CRITICAL)
>
> public void getBookingById() {
>
> given()
>
> .when()
>
> .get("/booking/" + bookingId)
>
> .then()
>
> .statusCode(200)
>
> .body("firstname", equalTo("James"))
>
> .body("lastname", equalTo("Brown"));
>
> }
>
> @Test(priority = 3, dependsOnMethods = "createBooking")
>
> @Story("Delete a booking")
>
> @Severity(SeverityLevel.NORMAL)
>
> public void deleteBooking() {
>
> given()
>
> .header("Cookie", "token=" + authToken)
>
> .when()
>
> .delete("/booking/" + bookingId)
>
> .then()
>
> .statusCode(201);
>
> }
>
> }

- [ ] BookingApiTest.java created

**8.3 BookingUITest.java (ui/)**

Tests the admin login flow via the browser. Navigates to the admin
panel, submits credentials, and asserts the rooms panel is visible. Any
failure automatically attaches a screenshot via UIBase:

> package com.demo.tests.ui;
>
> import com.demo.tests.base.UIBase;
>
> import io.qameta.allure.\*;
>
> import org.openqa.selenium.By;
>
> import org.openqa.selenium.WebElement;
>
> import org.openqa.selenium.support.ui.ExpectedConditions;
>
> import org.openqa.selenium.support.ui.WebDriverWait;
>
> import org.testng.Assert;
>
> import org.testng.annotations.Test;
>
> import java.time.Duration;
>
> @Epic("Booking UI")
>
> @Feature("Admin Panel")
>
> public class BookingUITest extends UIBase {
>
> @Test
>
> @Story("Admin can log in and view room list")
>
> @Severity(SeverityLevel.CRITICAL)
>
> @Description("Validates admin login flow and confirms rooms panel is
> visible")
>
> public void adminLoginAndViewRooms() {
>
> WebDriverWait wait = new WebDriverWait(driver,
> Duration.ofSeconds(10));
>
> driver.get(UI_URL + "/admin");
>
> driver.findElement(By.id("username")).sendKeys("admin");
>
> driver.findElement(By.id("password")).sendKeys("password");
>
> driver.findElement(By.id("doLogin")).click();
>
> WebElement roomsPanel = wait.until(
>
> ExpectedConditions.visibilityOfElementLocated(By.className("room-listing"))
>
> );
>
> Assert.assertTrue(roomsPanel.isDisplayed(),
>
> "Rooms panel should be visible after login");
>
> }
>
> }

- [ ] BookingUITest.java created

> **Section 9 — Run Tests & Generate the Report**

**9.1 Run the Full Test Suite**

> \# Full clean run (recommended each time for fresh results)
>
> ./gradlew clean test
>
> \# Expected output: test count, pass/skip/fail summary in terminal

- [ ] ./gradlew clean test completes without build errors

> **NOTE:** *If Gradle reports UP-TO-DATE and skips tests, always use
> './gradlew clean test' to force a fresh run.*

**9.2 Generate and Open the Allure Report**

> \# Serve live (opens browser automatically — best for demos)
>
> ./gradlew allureServe
>
> \# Or generate static HTML (useful for saving or CI artifacts)
>
> ./gradlew allureReport
>
> \# Output: build/reports/allure-report/allureReport/index.html

- [ ] Allure report opens in browser

- [ ] All tests visible in the Overview panel

**9.3 Allure Dashboard Walkthrough**

Use this table when presenting the dashboard. Each row is one talking
point:

| **Dashboard Panel** | **What It Shows** | **Demo Talking Point** |
|----|----|----|
| Overview | Pass/fail rate broken down by Epic \> Feature \> Story | Mirrors how a team organises test ownership across services |
| Suites | Each TestNG class and individual test execution time | Useful for spotting slow tests and optimisation opportunities |
| Behaviors | Tests grouped by @Epic / @Feature / @Story annotations | Shows coverage from a PM or QA Lead perspective |
| Timeline | Parallel execution visualised across threads | Demonstrates awareness of test efficiency and concurrency |
| Failed Test Drill-down | Full request/response body, stack trace, failure screenshot | This is the RCA story — see Section 9 for the full script |

**9.4 Root Cause Analysis (RCA) Demo Script**

This is a deliberate, controlled failure designed to demonstrate RCA.
Walk through these steps during the interview:

**Step 1 — Introduce a Known Failure**

In BookingApiTest.java, change line in createBooking():

> // Before (correct)
>
> .statusCode(201)
>
> // After (intentionally wrong — simulates a misconfigured assertion)
>
> .statusCode(200)

- [ ] Intentional failure introduced

**Step 2 — Re-run and Open the Report**

> ./gradlew clean test allureServe

- [ ] Failure appears in report

**Step 3 — RCA Talking Points in the Report**

Click the failing test in Allure and walk through each layer:

1.  Assertion layer: 'Expected status code 200 but was 201' — the exact
    line that failed

2.  Request layer: full HTTP POST body sent to /booking/ — confirm
    request was correct

3.  Response layer: HTTP 201 received from service — confirm service
    behaved correctly

4.  Conclusion: 'This is a test defect, not a service defect. The
    service returned the correct HTTP 201 Created. The assertion was
    misconfigured to expect 200. Root cause is in the test, not the
    system under test.'

> **TIP:** This distinction — test defect vs system defect — is exactly
> what interviewers want to hear. It shows you understand the difference
> between a failing test and a broken system.

- [ ] RCA walk-through rehearsed

**Step 4 — Restore the Correct Assertion**

> .statusCode(201) // restore
>
> ./gradlew clean test allureServe

- [ ] All tests green again

> **Section 10 — Optional: GitHub Actions CI Pipeline**

Adding CI demonstrates end-to-end pipeline awareness, which is a strong
differentiator. Create this file at the path shown:

**10.1 .github/workflows/test.yml**

> name: Test Suite
>
> on: \[push, pull_request\]
>
> jobs:
>
> test:
>
> runs-on: ubuntu-latest
>
> steps:
>
> \- uses: actions/checkout@v4
>
> \- uses: actions/setup-java@v4
>
> with:
>
> java-version: '17'
>
> distribution: 'temurin'
>
> \- name: Grant Gradle wrapper permissions
>
> run: chmod +x gradlew
>
> \- name: Start Restful-Booker Platform
>
> run: docker run -d -p 3003:3003
> mwinteringham/restfulbooker-platform:latest
>
> \- name: Wait for services to be ready
>
> run: sleep 10
>
> \- name: Run test suite
>
> run: ./gradlew clean test
>
> \- name: Generate Allure report
>
> run: ./gradlew allureReport
>
> \- name: Upload Allure report as artifact
>
> uses: actions/upload-artifact@v4
>
> with:
>
> name: allure-report
>
> path: build/reports/allure-report/allureReport

- [ ] GitHub repository created and code pushed

- [ ] Actions workflow file committed to .github/workflows/

- [ ] Pipeline runs green on first push

> **TIP:** The Allure report is uploaded as a downloadable artifact from
> the Actions run. In the interview, pull it up from the GitHub Actions
> tab to show the full CI-to-report pipeline.
>
> **Section 11 — Quick Reference**

**11.1 Key Commands**

> \# Start system under test
>
> docker start rbp
>
> \# Run tests
>
> ./gradlew clean test
>
> \# Open Allure report
>
> ./gradlew allureServe
>
> \# Generate static report
>
> ./gradlew allureReport
>
> \# Stop system under test
>
> docker stop rbp
>
> \# Full reset of SUT
>
> docker rm -f rbp && docker run -d --name rbp -p 3003:3003
> mwinteringham/restfulbooker-platform:latest

**11.2 Default Credentials**

|                    |                                                 |
|--------------------|-------------------------------------------------|
| **Admin Username** | admin                                           |
| **Admin Password** | password                                        |
| **UI URL**         | http://localhost:3003                           |
| **Auth Endpoint**  | http://localhost:3003/auth/login                |
| **Booking API**    | http://localhost:3003/booking/                  |
| **Swagger (each)** | http://localhost:3003/{service}/swagger-ui.html |

**11.3 Progress Tracker**

Use this summary checklist for a quick status view:

- [ ] Section 1 — Prerequisites installed and verified

- [ ] Section 3 — Docker container running, all services responding

- [ ] Section 4 — Gradle project initialised with build.gradle

- [ ] Section 5 — All source directories and testng.xml created

- [ ] Section 6 — ApiBase and UIBase in place

- [ ] Section 7 — Model classes created

- [ ] Section 8 — All three test classes created

- [ ] Section 9 — Tests run green, Allure report opens

- [ ] Section 9 — RCA demo rehearsed

- [ ] Section 10 — (Optional) CI pipeline runs clean

**Good luck with the interview!**
