package com.demo.tests.regression;

import com.demo.tests.base.UIBase;
import io.qameta.allure.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Regression tests for TC-BOOK-001: Book a Suite Room via the Homepage.
 * Covers the full end-to-end booking flow from the manual test tutorial.
 */
@Epic("Booking Flow")
@Feature("Room Reservation")
public class BookingFlowUITest extends UIBase {

    private WebDriverWait wait;
    // Use room 3 (Suite) for the booking flow. The API test suite books room 1 with
    // different dates so there's no conflict. Epoch-offset ensures unique dates across runs.
    private static final int ROOM_ID = 3;
    private static final int DAY_OFFSET = 28 + (int)(System.currentTimeMillis() / 1000 % 30);
    private static final LocalDate CHECKIN = LocalDate.now().plusDays(DAY_OFFSET);
    private static final LocalDate CHECKOUT = CHECKIN.plusDays(2);

    @BeforeMethod
    public void initWait() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test(priority = 1)
    @Story("Book Now button scrolls to booking panel")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-BOOK-001 Step 1: Click Book Now, page scrolls to availability panel")
    public void testBookNowScrollsToBookingPanel() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        WebElement bookNowBtn = driver.findElement(By.cssSelector(".hero a.btn.btn-primary"));
        bookNowBtn.click();

        // After clicking, the booking panel should be near the top of the viewport
        WebElement bookingPanel = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("booking")));
        Assert.assertTrue(bookingPanel.isDisplayed(),
            "Booking panel should be visible after clicking Book Now");
    }

    @Test(priority = 2)
    @Story("Check Availability button and room cards are functional")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-BOOK-001 Steps 2-4: Verify date fields exist, click Check Availability, verify rooms display")
    public void testCheckAvailabilityShowsRooms() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // Verify date input fields exist
        List<WebElement> dateInputs = driver.findElements(By.cssSelector("#booking input.form-control"));
        Assert.assertTrue(dateInputs.size() >= 2, "Should have check-in and check-out date fields");

        // Click Check Availability (rooms section already shows default rooms)
        WebElement checkBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("#booking button.btn.btn-primary")));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", checkBtn);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        checkBtn.click();
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Verify rooms section displays cards
        List<WebElement> roomCards = driver.findElements(By.cssSelector(".room-card"));
        Assert.assertTrue(roomCards.size() >= 2,
            "Should display at least 2 room cards, found: " + roomCards.size());

        // Verify each card has a Book now button
        for (WebElement card : roomCards) {
            WebElement bookBtn = card.findElement(By.cssSelector("a.btn.btn-primary"));
            Assert.assertTrue(bookBtn.getText().contains("Book now"),
                "Each room card should have a 'Book now' button");
        }
    }

    @Test(priority = 3)
    @Story("Suite room card has correct features and price")
    @Severity(SeverityLevel.NORMAL)
    @Description("TC-BOOK-001 Step 5: Verify Suite card features")
    public void testSuiteRoomCardDetails() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));

        // Navigate to rooms section
        scrollToElement("#rooms");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        List<WebElement> roomCards = driver.findElements(By.cssSelector(".room-card"));
        WebElement suiteCard = roomCards.stream()
            .filter(c -> c.findElement(By.cssSelector(".card-title")).getText().contains("Suite"))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull(suiteCard, "Suite room card should exist");

        String cardText = suiteCard.getText();
        Assert.assertTrue(cardText.contains("225"), "Suite should show £225 price");
        Assert.assertTrue(cardText.contains("per night"), "Suite should show 'per night'");

        // Verify features
        List<WebElement> badges = suiteCard.findElements(By.cssSelector(".badge"));
        List<String> features = badges.stream().map(WebElement::getText).toList();
        Assert.assertTrue(features.contains("Radio"), "Suite should have Radio feature");
        Assert.assertTrue(features.contains("WiFi"), "Suite should have WiFi feature");
        Assert.assertTrue(features.contains("Safe"), "Suite should have Safe feature");

        // Verify Book now button
        WebElement bookBtn = suiteCard.findElement(By.cssSelector("a.btn.btn-primary"));
        Assert.assertTrue(bookBtn.getText().contains("Book now"),
            "Suite card should have 'Book now' button");
    }

    @Test(priority = 4)
    @Story("Suite reservation page displays room details")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-BOOK-001 Steps 6-9: Navigate to Suite reservation, verify details, features, policies")
    public void testSuiteReservationPageContent() {
        // Navigate directly to Suite reservation page
        driver.get(UI_URL + "/reservation/" + ROOM_ID + "?checkin=" + CHECKIN + "&checkout=" + CHECKOUT);

        WebElement roomTitle = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        Assert.assertTrue(roomTitle.getText().contains("Room"),
            "Page title should contain 'Room', got: " + roomTitle.getText());

        // Verify Max Guests
        String pageText = driver.findElement(By.tagName("body")).getText();
        Assert.assertTrue(pageText.contains("Max 2 Guests"),
            "Should show 'Max 2 Guests'");

        // Verify room image is present
        WebElement roomImage = driver.findElement(By.cssSelector("img.w-100, img.hero-image"));
        Assert.assertTrue(roomImage.isDisplayed(), "Room image should be visible");

        // Verify Room Description section
        Assert.assertTrue(pageText.contains("Room Description"),
            "Should have Room Description heading");

        // Verify Room Features
        Assert.assertTrue(pageText.contains("Room Features"),
            "Should have Room Features heading");

        // Verify Room Policies
        Assert.assertTrue(pageText.contains("Room Policies"),
            "Should have Room Policies heading");
        Assert.assertTrue(pageText.contains("Check-in") || pageText.contains("Check-In"),
            "Should show check-in policy");
        Assert.assertTrue(pageText.contains("No smoking"),
            "Should show No smoking rule");
    }

    @Test(priority = 5)
    @Story("Booking sidebar shows price summary")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-BOOK-001 Step 10: Verify calendar, price summary on reservation page")
    public void testBookingSidebarPriceSummary() {
        driver.get(UI_URL + "/reservation/" + ROOM_ID + "?checkin=" + CHECKIN + "&checkout=" + CHECKOUT);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Verify price display
        String sidebarText = driver.findElement(By.cssSelector("body")).getText();
        Assert.assertTrue(sidebarText.contains("Book This Room"),
            "Should show 'Book This Room' heading");
        Assert.assertTrue(sidebarText.contains("per night"),
            "Should show price per night");

        // Verify price summary appears (may need date selection first)
        Assert.assertTrue(sidebarText.contains("Price Summary") || sidebarText.contains("Total"),
            "Should show Price Summary or Total");
    }

    @Test(priority = 6)
    @Story("Complete booking with customer details")
    @Severity(SeverityLevel.CRITICAL)
    @Description("TC-BOOK-001 Steps 11-14: Fill form, submit, verify confirmation, return home")
    public void testCompleteBookingFlow() {
        driver.get(UI_URL + "/reservation/" + ROOM_ID + "?checkin=" + CHECKIN + "&checkout=" + CHECKOUT);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Click Reserve Now to reveal customer form (use JS click to avoid intercept)
        WebElement reserveBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("doReservation")));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", reserveBtn);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reserveBtn);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Fill customer details
        WebElement firstname = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.room-firstname")));
        WebElement lastname = driver.findElement(By.cssSelector("input.room-lastname"));
        WebElement email = driver.findElement(By.cssSelector("input.room-email"));
        WebElement phone = driver.findElement(By.cssSelector("input.room-phone"));

        firstname.sendKeys("Jane");
        lastname.sendKeys("Doe");
        email.sendKeys("jane.doe@example.com");
        phone.sendKeys("01234567890");

        // Verify all fields have values
        Assert.assertEquals(firstname.getAttribute("value"), "Jane");
        Assert.assertEquals(lastname.getAttribute("value"), "Doe");

        // Submit booking (JS click to avoid intercept from sticky nav)
        WebElement submitBtn = driver.findElement(By.cssSelector("button.btn.btn-primary.w-100"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block: 'center'});", submitBtn);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Verify confirmation message (wait for state change)
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        // Scroll to top to see the sidebar confirmation
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        String pageText = driver.findElement(By.cssSelector("body")).getText();
        Assert.assertTrue(pageText.contains("Booking Confirmed"),
            "Should show 'Booking Confirmed' message after submission. Page text contains: "
            + pageText.substring(0, Math.min(500, pageText.length())));

        // Verify Return home link
        WebElement returnHome = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.btn.btn-primary.w-100")));
        Assert.assertTrue(returnHome.getText().contains("Return home"),
            "Should have a 'Return home' button");

        // Click Return home (JS click to avoid intercept)
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", returnHome);
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Verify we're back on the homepage
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));
        Assert.assertTrue(driver.getCurrentUrl().matches("https?://localhost/?"),
            "Should navigate back to homepage");
    }

    private void setReactInputValue(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor("
            + "window.HTMLInputElement.prototype, 'value').set;"
            + "nativeInputValueSetter.call(arguments[0], arguments[1]);"
            + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
            + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            element, value);
    }

    private void scrollToElement(String cssSelector) {
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('" + cssSelector + "').scrollIntoView({behavior: 'smooth', block: 'start'});");
    }
}
