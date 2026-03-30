package com.demo.tests.rca;

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

/**
 * RCA Demo: Intentional API test failure for Allure root cause analysis.
 *
 * PURPOSE: This test deliberately uses the WRONG expected status code (200 instead of 201)
 * when creating a booking. The Restful-Booker Platform correctly returns HTTP 201 Created,
 * but this test asserts 200 OK — a classic "test defect, not a system defect" scenario.
 *
 * In the Allure report, click the failing test to see:
 *   1. Assertion: "Expected status code 200 but was 201"
 *   2. Request body: the valid booking JSON sent to the API
 *   3. Response: HTTP 201 with the created booking — service behaved correctly
 *   4. Conclusion: the test assertion is wrong, not the system
 */
@Epic("RCA Demo")
@Feature("Intentional Failures")
public class RcaBookingApiTest extends ApiBase {

    @BeforeClass
    public void authenticate() {
        Response response = given()
            .header("Content-Type", "application/json")
            .body("{\"username\":\"admin\",\"password\":\"password\"}")
            .post(AUTH_URL + "/auth/login")
            .then()
            .extract().response();
        authToken = response.getCookie("token");
    }

    @Test
    @Story("RCA-001: Booking creation returns wrong status code")
    @Severity(SeverityLevel.CRITICAL)
    @Description("INTENTIONAL FAILURE — Asserts HTTP 200 but the API correctly returns HTTP 201 Created. "
        + "This demonstrates a test defect: the assertion is misconfigured, not the service. "
        + "Use the Allure report to walk through the request, response, and assertion layers.")
    public void createBookingExpectsWrongStatusCode() {
        String checkin = LocalDate.now().plusMonths(11).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String checkout = LocalDate.now().plusMonths(11).plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);

        Booking booking = new Booking(
            3, "RCA", "DemoUser", true,
            new BookingDates(checkin, checkout)
        );

        // BUG: Asserts 200 OK, but the API correctly returns 201 Created
        given()
            .header("Content-Type", "application/json")
            .body(booking)
            .when()
            .post(BOOKING_URL + "/booking/")
            .then()
            .statusCode(200)  // <-- INTENTIONALLY WRONG: should be 201
            .body("bookingid", notNullValue());
    }
}
