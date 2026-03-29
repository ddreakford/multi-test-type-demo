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
