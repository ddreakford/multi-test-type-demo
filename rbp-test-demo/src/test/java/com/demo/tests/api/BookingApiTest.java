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
