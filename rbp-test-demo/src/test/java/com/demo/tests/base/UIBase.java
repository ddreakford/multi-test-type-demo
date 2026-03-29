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
