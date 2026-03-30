package com.demo.tests.regression;

import com.demo.tests.base.UIBase;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Regression tests for TC-MSG-001: Submit a Contact Message via the Homepage Form.
 * Covers all 4 steps from the manual test tutorial.
 */
@Epic("Contact Form")
@Feature("Send Us a Message")
public class ContactFormUITest extends UIBase {

    private WebDriverWait wait;

    @BeforeMethod
    public void initWait() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test(priority = 1)
    @Story("Contact form is visible with all fields")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-MSG-001 Step 1: Navigate to contact form and verify all fields are present and empty")
    public void testContactFormVisible() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // Scroll to contact section
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('#contact').scrollIntoView({block: 'start'});");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        WebElement contactSection = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("contact")));
        Assert.assertTrue(contactSection.getText().contains("Send Us a Message"),
            "Should show 'Send Us a Message' heading");

        // Verify all fields exist and are empty
        WebElement name = driver.findElement(By.id("name"));
        WebElement email = driver.findElement(By.id("email"));
        WebElement phone = driver.findElement(By.id("phone"));
        WebElement subject = driver.findElement(By.id("subject"));
        WebElement description = driver.findElement(By.id("description"));

        Assert.assertTrue(name.isDisplayed(), "Name field should be visible");
        Assert.assertTrue(email.isDisplayed(), "Email field should be visible");
        Assert.assertTrue(phone.isDisplayed(), "Phone field should be visible");
        Assert.assertTrue(subject.isDisplayed(), "Subject field should be visible");
        Assert.assertTrue(description.isDisplayed(), "Message field should be visible");

        Assert.assertEquals(name.getAttribute("value"), "", "Name field should be empty");
        Assert.assertEquals(email.getAttribute("value"), "", "Email field should be empty");

        WebElement submitBtn = contactSection.findElement(By.cssSelector("button.btn.btn-primary"));
        Assert.assertTrue(submitBtn.isDisplayed(), "Submit button should be visible");
    }

    @Test(priority = 2)
    @Story("Fill and submit contact form")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-MSG-001 Steps 2-3: Fill form fields and submit, verify confirmation message")
    public void testFillAndSubmitContactForm() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // Scroll to contact section
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('#contact').scrollIntoView({block: 'start'});");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Fill in form fields
        WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        WebElement email = driver.findElement(By.id("email"));
        WebElement phone = driver.findElement(By.id("phone"));
        WebElement subject = driver.findElement(By.id("subject"));
        WebElement description = driver.findElement(By.id("description"));

        name.sendKeys("John Smith");
        email.sendKeys("john.smith@example.com");
        phone.sendKeys("01234987654");
        subject.sendKeys("Booking Inquiry - Anniversary Weekend");
        description.sendKeys("Hello, my wife and I are celebrating our 10th anniversary "
            + "and would like to book your Suite. Do you have availability?");

        // Verify fields accepted input
        Assert.assertEquals(name.getAttribute("value"), "John Smith");
        Assert.assertEquals(email.getAttribute("value"), "john.smith@example.com");

        // Submit the form (JS click to avoid sticky nav intercept)
        WebElement submitBtn = driver.findElement(
            By.cssSelector("#contact button.btn.btn-primary"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", submitBtn);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Verify confirmation message
        WebElement contactSection = driver.findElement(By.id("contact"));
        String confirmText = contactSection.getText();
        Assert.assertTrue(confirmText.contains("Thanks for getting in touch"),
            "Should show 'Thanks for getting in touch' message, got: " + confirmText);
        Assert.assertTrue(confirmText.contains("John Smith"),
            "Confirmation should include the submitted name");
        Assert.assertTrue(confirmText.contains("Booking Inquiry"),
            "Confirmation should reference the submitted subject");
    }

    @Test(priority = 3)
    @Story("Form resets after page reload")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-MSG-001 Step 4: Navigate back to homepage and verify form is reset")
    public void testFormResetsAfterNavigation() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // First submit a message so the form shows confirmation
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('#contact').scrollIntoView({block: 'start'});");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        name.sendKeys("Test User");
        driver.findElement(By.id("email")).sendKeys("test@example.com");
        driver.findElement(By.id("phone")).sendKeys("01234000000");
        driver.findElement(By.id("subject")).sendKeys("Test Subject");
        driver.findElement(By.id("description")).sendKeys("Test message body");
        WebElement btn = driver.findElement(By.cssSelector("#contact button.btn.btn-primary"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", btn);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Navigate back to homepage
        driver.findElement(By.cssSelector("a.navbar-brand")).click();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // Scroll to contact and verify form is reset
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('#contact').scrollIntoView({block: 'start'});");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        WebElement nameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        Assert.assertEquals(nameField.getAttribute("value"), "",
            "Name field should be empty after navigating back");
    }
}
