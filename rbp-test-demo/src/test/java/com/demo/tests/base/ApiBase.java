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
