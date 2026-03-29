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
