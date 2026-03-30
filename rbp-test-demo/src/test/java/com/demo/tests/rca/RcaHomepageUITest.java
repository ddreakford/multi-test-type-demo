package com.demo.tests.rca;

import com.demo.tests.base.UIBase;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * RCA Demo: Intentional UI test failure for Allure root cause analysis.
 *
 * PURPOSE: This test deliberately asserts that the homepage heading contains
 * "Welcome to Grand Hotel" when the actual text is "Welcome to Shady Meadows B&B".
 * This is a classic "test defect" — the tester hardcoded the wrong expected value.
 *
 * In the Allure report, click the failing test to see:
 *   1. Assertion: expected text vs actual text
 *   2. Failure screenshot: shows the actual homepage with "Shady Meadows B&B"
 *   3. Conclusion: the test has a wrong expected value, the UI is correct
 */
@Epic("RCA Demo")
@Feature("Intentional Failures")
public class RcaHomepageUITest extends UIBase {

    @Test
    @Story("RCA-002: Homepage heading asserts wrong hotel name")
    @Severity(SeverityLevel.CRITICAL)
    @Description("INTENTIONAL FAILURE — Asserts the heading contains 'Welcome to Grand Hotel' "
        + "but the actual text is 'Welcome to Shady Meadows B&B'. This demonstrates a test defect: "
        + "the expected value is wrong, not the UI. The failure screenshot in Allure confirms the UI is correct.")
    public void homepageHeadingExpectsWrongHotelName() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        WebElement heading = driver.findElement(By.cssSelector(".hero .display-4"));
        String actualText = heading.getText();

        // BUG: Asserts wrong hotel name — actual is "Shady Meadows B&B"
        Assert.assertTrue(actualText.contains("Welcome to Grand Hotel"),
            "Expected heading to contain 'Welcome to Grand Hotel' but found: '" + actualText + "'. "
            + "This is an intentional test defect for RCA demonstration — the UI is correct.");
    }
}
