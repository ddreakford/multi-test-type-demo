package com.demo.tests.regression;

import com.demo.tests.base.UIBase;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Regression tests for TC-UI-001: Validate Restful-Booker Platform Homepage Elements.
 * Covers all 9 steps from the manual test tutorial.
 */
@Epic("Homepage UI")
@Feature("Homepage Validation")
public class HomepageUITest extends UIBase {

    private WebDriverWait wait;

    @BeforeMethod
    public void initWait() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test(priority = 1)
    @Story("Homepage loads successfully")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-UI-001 Step 1: Navigate to homepage and verify it loads")
    public void testHomepageLoads() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));
        Assert.assertTrue(driver.getTitle().length() > 0, "Page should have a title");
    }

    @Test(priority = 2)
    @Story("Hero section displays welcome text")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-UI-001 Steps 2-3: Verify welcome heading, description, and Book Now button")
    public void testHeroSectionContent() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        WebElement heading = driver.findElement(By.cssSelector(".hero .display-4"));
        Assert.assertTrue(heading.getText().contains("Welcome to Shady Meadows B&B"),
            "Hero heading should contain 'Welcome to Shady Meadows B&B', got: " + heading.getText());

        WebElement description = driver.findElement(By.cssSelector(".hero .lead"));
        Assert.assertTrue(description.getText().contains("delightful Bed & Breakfast"),
            "Hero description should mention 'delightful Bed & Breakfast'");
        Assert.assertTrue(description.getText().contains("Newingtonfordburyshire"),
            "Hero description should mention 'Newingtonfordburyshire'");

        WebElement bookNowBtn = driver.findElement(By.cssSelector(".hero a.btn.btn-primary"));
        Assert.assertTrue(bookNowBtn.isDisplayed(), "Book Now button should be visible");
        Assert.assertEquals(bookNowBtn.getText().trim(), "Book Now");
    }

    @Test(priority = 3)
    @Story("Navigation bar has correct links")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-UI-001 Step 4: Verify nav bar contains expected links")
    public void testNavigationBarElements() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".navbar")));

        WebElement brandLink = driver.findElement(By.cssSelector("a.navbar-brand"));
        Assert.assertTrue(brandLink.getText().contains("Shady Meadows"),
            "Brand link should contain 'Shady Meadows'");

        List<WebElement> navLinks = driver.findElements(By.cssSelector(".nav-link"));
        List<String> linkTexts = navLinks.stream().map(WebElement::getText).toList();

        Assert.assertTrue(linkTexts.stream().anyMatch(t -> t.contains("Rooms")), "Nav should have Rooms link");
        Assert.assertTrue(linkTexts.stream().anyMatch(t -> t.contains("Booking")), "Nav should have Booking link");
        Assert.assertTrue(linkTexts.stream().anyMatch(t -> t.contains("Location")), "Nav should have Location link");
        Assert.assertTrue(linkTexts.stream().anyMatch(t -> t.contains("Contact")), "Nav should have Contact link");
        Assert.assertTrue(linkTexts.stream().anyMatch(t -> t.contains("Admin")), "Nav should have Admin link");
    }

    @Test(priority = 4)
    @Story("Check Availability panel is present")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-UI-001 Step 5: Verify booking panel with date fields and button")
    public void testCheckAvailabilityPanel() {
        WebElement bookingSection = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("booking")));
        Assert.assertTrue(bookingSection.isDisplayed(), "Booking section should be visible");

        WebElement title = bookingSection.findElement(By.cssSelector(".card-title"));
        Assert.assertTrue(title.getText().contains("Check Availability"),
            "Booking panel title should contain 'Check Availability'");

        List<WebElement> dateInputs = bookingSection.findElements(By.cssSelector("input.form-control"));
        Assert.assertTrue(dateInputs.size() >= 2,
            "Should have at least 2 date input fields, found: " + dateInputs.size());

        WebElement checkBtn = bookingSection.findElement(By.cssSelector("button.btn.btn-primary"));
        Assert.assertTrue(checkBtn.isDisplayed(), "Check Availability button should be visible");
    }

    @Test(priority = 5)
    @Story("Our Rooms section displays room cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-UI-001 Step 6: Verify three room cards with prices and features")
    public void testRoomCardsDisplay() {
        WebElement roomsSection = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("rooms")));

        WebElement heading = roomsSection.findElement(By.cssSelector(".display-5"));
        Assert.assertTrue(heading.getText().contains("Our Rooms"),
            "Section heading should be 'Our Rooms'");

        List<WebElement> roomCards = driver.findElements(By.cssSelector(".room-card"));
        Assert.assertEquals(roomCards.size(), 3,
            "Should display exactly 3 room cards, found: " + roomCards.size());

        // Verify each expected room type exists
        List<String> roomTitles = roomCards.stream()
            .map(card -> card.findElement(By.cssSelector(".card-title")).getText())
            .toList();
        Assert.assertTrue(roomTitles.contains("Single"), "Should have a Single room card");
        Assert.assertTrue(roomTitles.contains("Double"), "Should have a Double room card");
        Assert.assertTrue(roomTitles.contains("Suite"), "Should have a Suite room card");

        // Verify each card has a Book now button and a price
        for (WebElement card : roomCards) {
            WebElement bookBtn = card.findElement(By.cssSelector("a.btn.btn-primary"));
            Assert.assertTrue(bookBtn.isDisplayed(), "Each room card should have a Book now button");
            Assert.assertTrue(bookBtn.getText().contains("Book now"),
                "Button text should be 'Book now'");

            WebElement price = card.findElement(By.cssSelector(".card-footer"));
            Assert.assertTrue(price.getText().contains("per night"),
                "Each card should show price per night");
        }
    }

    @Test(priority = 6)
    @Story("Our Location section displays map and contact info")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-UI-001 Step 7: Verify location section with map and contact details")
    public void testLocationSection() {
        WebElement locationSection = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("location")));

        WebElement heading = locationSection.findElement(By.cssSelector(".display-5"));
        Assert.assertTrue(heading.getText().contains("Our Location"),
            "Section heading should be 'Our Location'");

        // Verify contact information is present
        String sectionText = locationSection.getText();
        Assert.assertTrue(sectionText.contains("Contact Information"),
            "Should show Contact Information heading");
        Assert.assertTrue(sectionText.contains("Address"),
            "Should show Address heading");
        Assert.assertTrue(sectionText.contains("Phone"),
            "Should show Phone heading");
        Assert.assertTrue(sectionText.contains("Email"),
            "Should show Email heading");
        Assert.assertTrue(sectionText.contains("Getting Here"),
            "Should show Getting Here heading");
    }

    @Test(priority = 7)
    @Story("Contact form is present with all fields")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-UI-001 Step 8: Verify Send Us a Message form fields")
    public void testContactFormPresence() {
        WebElement contactSection = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("contact")));

        String sectionText = contactSection.getText();
        Assert.assertTrue(sectionText.contains("Send Us a Message"),
            "Should show 'Send Us a Message' heading");

        Assert.assertTrue(driver.findElement(By.id("name")).isDisplayed(),
            "Name field should be present");
        Assert.assertTrue(driver.findElement(By.id("email")).isDisplayed(),
            "Email field should be present");
        Assert.assertTrue(driver.findElement(By.id("phone")).isDisplayed(),
            "Phone field should be present");
        Assert.assertTrue(driver.findElement(By.id("subject")).isDisplayed(),
            "Subject field should be present");
        Assert.assertTrue(driver.findElement(By.id("description")).isDisplayed(),
            "Message field should be present");

        WebElement submitBtn = contactSection.findElement(By.cssSelector("button.btn.btn-primary"));
        Assert.assertTrue(submitBtn.isDisplayed(), "Submit button should be visible");
    }

    @Test(priority = 8)
    @Story("Footer displays hotel info and links")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-UI-001 Step 9: Verify footer content")
    public void testFooterContent() {
        WebElement footer = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.tagName("footer")));

        String footerText = footer.getText();
        Assert.assertTrue(footerText.contains("Shady Meadows"),
            "Footer should contain hotel name");
        Assert.assertTrue(footerText.contains("Contact Us"),
            "Footer should have Contact Us section");
        Assert.assertTrue(footerText.contains("Quick Links"),
            "Footer should have Quick Links section");
    }
}
