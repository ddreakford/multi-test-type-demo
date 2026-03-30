package com.demo.tests.screenshots;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Captures screenshots for the manual test tutorial.
 * Run with: ./gradlew test --tests "com.demo.tests.screenshots.ManualTestScreenshots"
 */
public class ManualTestScreenshots {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost";
    private static final String SCREENSHOT_DIR = "../docs/screenshots/";

    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        if ("true".equals(System.getenv("CI"))) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        }
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Ensure screenshot directory exists
        new File(SCREENSHOT_DIR).mkdirs();
    }

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }

    // ========================================================================
    // SCENARIO 1: Validate the Restful-Booker Platform UI
    // ========================================================================

    @Test(priority = 1)
    public void scenario1_01_homepage() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));
        sleep(2000); // Let images and map load
        saveScreenshot("S1_01_homepage_full");
    }

    @Test(priority = 2, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_02_heroSection() {
        // Scroll to top to show hero with welcome text and Book Now button
        scrollToTop();
        sleep(500);
        saveScreenshot("S1_02_hero_welcome_booknow");
    }

    @Test(priority = 3, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_03_bookingPanel() {
        // Scroll to the booking/availability section
        scrollToElement("#booking");
        sleep(500);
        saveScreenshot("S1_03_booking_panel");
    }

    @Test(priority = 4, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_04_ourRooms() {
        scrollToElement("#rooms");
        sleep(500);
        saveScreenshot("S1_04_our_rooms");
    }

    @Test(priority = 5, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_05_ourLocation() {
        scrollToElement("#location");
        sleep(1000); // Map tiles need time
        saveScreenshot("S1_05_our_location");
    }

    @Test(priority = 6, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_06_contactForm() {
        scrollToElement("#contact");
        sleep(500);
        saveScreenshot("S1_06_contact_form");
    }

    @Test(priority = 7, dependsOnMethods = "scenario1_01_homepage")
    public void scenario1_07_footer() {
        scrollToBottom();
        sleep(500);
        saveScreenshot("S1_07_footer");
    }

    // ========================================================================
    // SCENARIO 2: Book Now Flow
    // ========================================================================

    @Test(priority = 10)
    public void scenario2_01_clickBookNow() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));
        sleep(1000);

        // Click the "Book Now" button in the hero section
        WebElement bookNowBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(".hero a.btn.btn-primary"))
        );
        bookNowBtn.click();
        sleep(1000);
        saveScreenshot("S2_01_after_booknow_click");
    }

    @Test(priority = 11, dependsOnMethods = "scenario2_01_clickBookNow")
    public void scenario2_02_selectDatesAndCheckAvailability() {
        // Use JavaScript to set date values in the React date pickers
        LocalDate checkin = LocalDate.now().plusWeeks(2);
        LocalDate checkout = checkin.plusDays(3);
        String checkinStr = checkin.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String checkoutStr = checkout.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        List<WebElement> dateInputs = driver.findElements(By.cssSelector("#booking input.form-control"));
        if (dateInputs.size() >= 2) {
            setReactInputValue(dateInputs.get(0), checkinStr);
            sleep(300);
            setReactInputValue(dateInputs.get(1), checkoutStr);
            sleep(500);
        }
        saveScreenshot("S2_02_dates_selected");

        // Click "Check Availability"
        WebElement checkBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("#booking button.btn.btn-primary"))
        );
        checkBtn.click();
        sleep(2000);
        scrollToElement("#rooms");
        sleep(500);
        saveScreenshot("S2_03_rooms_available");
    }

    @Test(priority = 12, dependsOnMethods = "scenario2_02_selectDatesAndCheckAvailability")
    public void scenario2_03_clickBookSuite() {
        List<WebElement> roomCards = driver.findElements(By.cssSelector(".room-card"));
        WebElement suiteBookBtn = null;

        for (WebElement card : roomCards) {
            String title = card.findElement(By.cssSelector(".card-title")).getText();
            if (title.contains("Suite")) {
                suiteBookBtn = card.findElement(By.cssSelector("a.btn.btn-primary"));
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});", card);
                sleep(500);
                saveScreenshot("S2_04_suite_card");
                break;
            }
        }

        if (suiteBookBtn != null) {
            suiteBookBtn.click();
        } else {
            // Fallback: navigate directly to suite reservation page
            // Room 3 is typically the Suite
            LocalDate checkin = LocalDate.now().plusWeeks(2);
            LocalDate checkout = checkin.plusDays(3);
            driver.get(BASE_URL + "/reservation/3?checkin=" + checkin + "&checkout=" + checkout);
        }
        sleep(2000);
        saveScreenshot("S2_05_reservation_page");
    }

    @Test(priority = 13, dependsOnMethods = "scenario2_03_clickBookSuite")
    public void scenario2_04_roomDetailsAndFeatures() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1")));
        sleep(1000);
        saveScreenshot("S2_06_room_details_top");

        // Scroll to features/policies
        ((JavascriptExecutor) driver).executeScript(
            "window.scrollTo(0, document.body.scrollHeight / 2);");
        sleep(500);
        saveScreenshot("S2_07_room_features_policies");
    }

    @Test(priority = 14, dependsOnMethods = "scenario2_04_roomDetailsAndFeatures")
    public void scenario2_05_fillBookingAndConfirm() {
        scrollToTop();
        sleep(1000);

        // Click "Reserve Now" to reveal the booking form
        try {
            WebElement reserveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("doReservation")));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});", reserveBtn);
            sleep(500);
            saveScreenshot("S2_08_before_reserve");
            reserveBtn.click();
            sleep(1000);
        } catch (Exception e) {
            // Button might have different selector
            List<WebElement> btns = driver.findElements(By.cssSelector("button.btn.btn-primary.w-100"));
            if (!btns.isEmpty()) {
                btns.get(0).click();
                sleep(1000);
            }
        }

        // Fill customer details
        try {
            WebElement firstname = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.room-firstname")));
            WebElement lastname = driver.findElement(By.cssSelector("input.room-lastname"));
            WebElement email = driver.findElement(By.cssSelector("input.room-email"));
            WebElement phone = driver.findElement(By.cssSelector("input.room-phone"));

            firstname.sendKeys("Jane");
            lastname.sendKeys("Doe");
            email.sendKeys("jane.doe@example.com");
            phone.sendKeys("01onal234567");
            sleep(500);
            saveScreenshot("S2_09_booking_form_filled");

            // Submit
            WebElement submitBtn = driver.findElement(By.cssSelector("button.btn.btn-primary.w-100"));
            submitBtn.click();
            sleep(2000);
            scrollToTop();
            sleep(500);
            saveScreenshot("S2_10_booking_confirmation");
        } catch (TimeoutException e) {
            saveScreenshot("S2_09_booking_form_state");
        }
    }

    // ========================================================================
    // SCENARIO 3: Send Us a Message
    // ========================================================================

    @Test(priority = 20)
    public void scenario3_01_navigateToContactForm() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".hero")));
        sleep(1000);

        // Scroll to the contact section
        scrollToElement("#contact");
        sleep(500);
        saveScreenshot("S3_01_contact_form_empty");
    }

    @Test(priority = 21, dependsOnMethods = "scenario3_01_navigateToContactForm")
    public void scenario3_02_fillContactForm() {
        WebElement name = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        WebElement email = driver.findElement(By.id("email"));
        WebElement phone = driver.findElement(By.id("phone"));
        WebElement subject = driver.findElement(By.id("subject"));
        WebElement description = driver.findElement(By.id("description"));

        name.sendKeys("John Smith");
        email.sendKeys("john.smith@example.com");
        phone.sendKeys("01onal987654");
        subject.sendKeys("Booking Inquiry - Anniversary Weekend");
        description.sendKeys("Hello, my wife and I are celebrating our 10th "
            + "anniversary and would like to book your Suite for the weekend "
            + "of the 15th. Do you have availability? We would also appreciate "
            + "any recommendations for local restaurants. Thank you!");

        sleep(500);
        saveScreenshot("S3_02_contact_form_filled");
    }

    @Test(priority = 22, dependsOnMethods = "scenario3_02_fillContactForm")
    public void scenario3_03_submitContactForm() {
        // Click Submit
        WebElement submitBtn = driver.findElement(
            By.cssSelector("#contact button.btn.btn-primary"));
        submitBtn.click();
        sleep(2000);
        saveScreenshot("S3_03_contact_form_submitted");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void saveScreenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Paths.get(SCREENSHOT_DIR + name + ".png");
            Files.copy(src.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + dest);
        } catch (IOException e) {
            System.err.println("Failed to save screenshot: " + name + " - " + e.getMessage());
        }
    }

    private void scrollToElement(String cssSelector) {
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('" + cssSelector + "').scrollIntoView({behavior: 'smooth', block: 'start'});");
    }

    private void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    private void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    private void setReactInputValue(WebElement element, String value) {
        // React date pickers need native input setter to trigger onChange
        ((JavascriptExecutor) driver).executeScript(
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor("
            + "window.HTMLInputElement.prototype, 'value').set;"
            + "nativeInputValueSetter.call(arguments[0], arguments[1]);"
            + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
            + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            element, value);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
