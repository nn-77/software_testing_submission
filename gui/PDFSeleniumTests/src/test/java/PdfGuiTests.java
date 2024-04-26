import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class PdfGuiTests {
    // In this class, we test the main page [http://localhost:8080/] as well as 1 sub-module (PDF To Image) [http://localhost:8080/pdf-to-img]
    // thoroughly since there are many tools [and it would be infeasible to test them all].

    // We test the main page thoroughly with respect to 3 sub-modules (View PDF, Remove, and Auto Redact) to
    // make the main page tests substantial and generalizable.

    // Note that there is no coverage criterion "number" to hit here - the goal of this is to ensure that things
    // that must work on the site 1) do work as of now, and 2) do not break when future changes are added (i.e. these tests,
    // especially with the page object model, should not fail when new features are added)

    // The tests that fail are ones that reveal faults - see the report we submitted as well as the comments above these failing tests
    // These tests are tagged with the tag "Fault"

    // there are 2 page object models (in the files "./MainPageObjectModel.java" and "./PdfToImagePageObjectModel.java")
    // there are 2 "infra" methods (setup and teardown)
    // there are 2 main sections for the tests below - one for the landing page tests and one for the PDF to image sub tool

    // In general, the name of each test describes what it does
    // Each of the 2 sections below describes what it does in more detail (with comments)


    WebDriver driver;
    String base = "http://localhost:8080/";

    MainPageObjectModel mainPage;
    PdfToImagePageObjectModel pdfToImagePage;


    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        driver.get(base);
        mainPage = new MainPageObjectModel(driver);
        pdfToImagePage = new PdfToImagePageObjectModel(driver);
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    /////////////////////////////////////////////////////////////////////////
//    @BeforeAll
//    static void setup() {
//        driver = new ChromeDriver();
//        driver.get(base);
//    }
//    @AfterEach
//    void teardown() {
//        driver.manage().deleteAllCookies();
//        int numWindows = driver.getWindowHandles().size();
//        Arrays.stream(driver.getWindowHandles().toArray()).limit(numWindows-1)
//                .sequential().forEach(w -> {driver.switchTo().window((String) w); driver.close();});
//        driver.switchTo().window((String) (driver.getWindowHandles().toArray())[0]);
//        driver.get(base);
//    }

//    @AfterAll
//    static void finalTeardown() {
//        driver.quit();
//    }
    /////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // MAIN/FRONT PAGE TESTS //

    // As stated at top of file, we test attributes on the main page only related to components exclusive to the main page
    // as well as features related to the 3 tools (View PDF, Remove, Auto Redact) listed to make the main page tests substantial and generalizable.

    // As noted at the top of this file, there is no coverage criterion "number" to hit, and the goal is to make sure
    // functionality that should be available on the site does not ever break.
    // The name of each test describes what it does

    @Test
    void clickingLogoStaysOnHome() {
        WebElement logo = mainPage.getLogo();
        logo.click();

        assertEquals(base, driver.getCurrentUrl());
    }

    // there are logos at the bottom of the main page with links to github, docker, etc.
    @Test
    void verifyBottomLinksAreBelowMainToolContainer() {
        WebElement mainFunctionalityContainer = mainPage.getMainDivContainer();

        WebElement git = mainPage.getGithubLink();
        WebElement dock = mainPage.getDockerLink();
        WebElement discord = mainPage.getDiscordLink();
        WebElement sponsor = mainPage.getSponsorLink();
        WebElement licenses = mainPage.getLicensesLink();

        int mainContainerY = mainFunctionalityContainer.getLocation().y;
        int mainContainerHeight = mainFunctionalityContainer.getSize().height;

        // need to be at the bottom of the page (below main container)
        assertTrue(git.getLocation().y > mainContainerY + mainContainerHeight);
        assertTrue(dock.getLocation().y > mainContainerY + mainContainerHeight);
        assertTrue(discord.getLocation().y > mainContainerY + mainContainerHeight);
        assertTrue(sponsor.getLocation().y > mainContainerY + mainContainerHeight);
        assertTrue(licenses.getLocation().y > mainContainerY + mainContainerHeight);

        // need to be shown
        assertTrue(git.isDisplayed());
        assertTrue(dock.isDisplayed());
        assertTrue(discord.isDisplayed());
        assertTrue(sponsor.isDisplayed());
        assertTrue(licenses.isDisplayed());
    }

    @Test
    void clickingGithubLinkGoesToAppropriatePage() {
        WebElement git = mainPage.getGithubLink();

        Actions actions = new Actions(driver);
        actions.moveToElement(git).click().perform();

        String s = (String) (driver.getWindowHandles().toArray())[1];
        driver.switchTo().window(s);

        assertEquals("https://github.com/Stirling-Tools/Stirling-PDF", driver.getCurrentUrl());
    }

    @Test
    void clickingDockerLinkGoesToAppropriatePage() {
        WebElement dock = mainPage.getDockerLink();

        Actions actions = new Actions(driver);
        actions.moveToElement(dock).click().perform();

        String s = (String) (driver.getWindowHandles().toArray())[1];
        driver.switchTo().window(s);

        assertEquals("https://hub.docker.com/r/frooodle/s-pdf", driver.getCurrentUrl());
    }

    @Test
    void clickingDiscordLinkGoesToAppropriatePage() {
        WebElement discord = mainPage.getDiscordLink();

        Actions actions = new Actions(driver);
        actions.moveToElement(discord).click().perform();

        String s = (String) (driver.getWindowHandles().toArray())[1];
        driver.switchTo().window(s);

        assertTrue(driver.getCurrentUrl().contains("https://discord.com/invite/"));
    }

    @Test
    void clickingSponsorLinkGoesToAppropriatePage() {
        WebElement sponsor = mainPage.getSponsorLink();

        Actions actions = new Actions(driver);
        actions.moveToElement(sponsor).click().perform();

        String s = (String) (driver.getWindowHandles().toArray())[1];
        driver.switchTo().window(s);

        assertEquals("https://github.com/sponsors/Frooodle", driver.getCurrentUrl());
    }

    @Test
    void ensureTaglineExistsAndIsCorrect() {
        WebElement tag = mainPage.getTagline();

        assertTrue(tag.getText().contains("Your locally hosted one-stop-shop for all your PDF needs"));
    }

    // relevant tools here are {Remove}
    @Test
    void ensureRelevantToolsAreInPageOperationsDropdown() {
        // makes the dropdown visible (clicks it) and returns the appropriate element
        WebElement rm = mainPage.getPageOperationsDropdown_remove();

        assertTrue(rm.isDisplayed());
        assertEquals("Remove", rm.getText());

        rm.click();
        assertTrue(driver.getCurrentUrl().contains("remove-pages"));
    }

    // relevant tools here are {Auto Redact}
    @Test
    void ensureRelevantToolsAreInSecurityDropdown() {
        // makes the dropdown visible (clicks it) and returns the appropriate element
        WebElement redact = mainPage.getSecurityDropdown_autoredact();

        assertTrue(redact.isDisplayed());
        assertEquals("Auto Redact", redact.getText());

        redact.click();
        assertTrue(driver.getCurrentUrl().contains("auto-redact"));
    }

    @Test
    void ensureRelevantToolsAreInMiscellaneousDropdown() {
        // makes the dropdown visible (clicks it) and returns the appropriate element
        WebElement view = mainPage.getMiscellaneousDropdown_view();

        assertTrue(view.isDisplayed());
        assertEquals("View PDF", view.getText());

        view.click();
        assertTrue(driver.getCurrentUrl().contains("view-pdf"));
    }

    // THIS TEST REVEALS A FAULT //
    // when a tool on the main page is "favorited", it moves to the top of the list/container. When it is unfavorited,
        // it remains at the top of the list/container. However, when the page is refreshed, the tool's card goes back
        // to its original position in the list. So, the intended behavior was for the card to go back to its original
        // position immediately upon being unfavorited, without needing a refresh. It does not do this.
    @Test
    @Tag("Fault")
    void favoriteThenUnfavoritePutsCardBackAtInitialLocation() {
        // hover over the card we want to favorite (so the star is shown)
        WebElement card = mainPage.getRemoveCard();
        Actions actions = new Actions(driver);
        actions.moveToElement(card).perform();

        // click favorite on a card in the middle of the container
        WebElement favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[17]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        // hover over the card we want to un-favorite (so the star is shown)
        card = mainPage.getRemoveCard();
        actions = new Actions(driver);
        actions.moveToElement(card).perform();

        // un-favorite that same card
        favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[2]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        // now the "remove" card should be back in the middle of the list, but it is not
        WebElement container = mainPage.getToolCardContainer();

        List<WebElement> children = container.findElements(By.xpath("./*"));
        WebElement shouldBeRemove = children.get(16);
        assertTrue(shouldBeRemove.getText().contains("Remove"));
    }

    @Test
    void ensureFavoritesGetAddedToTopRightFavoritesDropdown() {
        WebElement card = mainPage.getRemoveCard();
        Actions actions = new Actions(driver);
        actions.moveToElement(card).perform();

        WebElement favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[17]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        WebElement favDropdown = mainPage.getFavoritesDropdownToolbar();
        actions = new Actions(driver);
        actions.moveToElement(favDropdown).click().perform();

        WebElement container = mainPage.getFavoritesDropdownContentList();

        List<WebElement> children = container.findElements(By.xpath("./*"));

        // dropdown should only have 1 tool
        assertEquals(1, children.size());
        // and that tool should be the "remove" tool
        actions = new Actions(driver);
        actions.moveToElement(children.getFirst()).click().perform();
        assertTrue(driver.getCurrentUrl().contains("remove-pages"));
    }

    @Test
    void ensureFavoritesGetRemovedFromTopRightFavoritesDropdownWhenUnFavorited() {
        // favorite
        WebElement card = mainPage.getRemoveCard();
        Actions actions = new Actions(driver);
        actions.moveToElement(card).perform();

        WebElement favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[17]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        // un favorite
        card = mainPage.getRemoveCard();
        actions = new Actions(driver);
        actions.moveToElement(card).perform();

        favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[2]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        // get the dropdown for favorites
        WebElement favDropdown = mainPage.getFavoritesDropdownToolbar();
        actions = new Actions(driver);
        actions.moveToElement(favDropdown).click().perform();

        // ensure it says "No favorites added" in the dropdown
        WebElement onlyListItem = driver.findElement(By.xpath("/html/body/div/div/div[1]/nav/div/div/ul[2]/li[1]/div/a"));
        assertTrue(onlyListItem.getText().contains("No favorites added"));
    }

    @Test
    void ensureFavoritedToolStaysAtTopUponRefresh() {
        // favorite
        WebElement card = mainPage.getRemoveCard();
        Actions actions = new Actions(driver);
        actions.moveToElement(card).perform();

        WebElement favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[17]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();

        driver.navigate().refresh();

        WebElement container = mainPage.getToolCardContainer();

        List<WebElement> children = container.findElements(By.xpath("./*"));
        WebElement shouldBeRemove = children.get(1);
        assertTrue(shouldBeRemove.getText().contains("Remove"));
    }

    // relevant tools here are {View PDF, Remove, Auto Redact}
    @Test
    void ensureRelevantToolsAreInMainContainer() {
        List<String> toCheck = List.of("Remove", "View PDF", "Auto Redact");

        WebElement container = mainPage.getToolCardContainer();
        List<WebElement> children = container.findElements(By.xpath("./*"));
        List<String> childTexts = children.stream().map(WebElement::getText).toList();

        List<String> notFoundTools = toCheck.stream().filter(tool -> childTexts.stream().noneMatch(text -> text.contains(tool))).toList();

        assertTrue(notFoundTools.isEmpty());
    }

    // relevant tools here are {View PDF, Remove, Auto Redact}
    @Test
    void ensureRelevantToolsInMainContainerGoToCorrectPage() {
        WebElement view = mainPage.getViewCard();
        WebElement remove = mainPage.getRemoveCard();
        WebElement redact = mainPage.getRedactCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(view).click().perform();
        assertTrue(driver.getCurrentUrl().contains("view-pdf"));
        driver.navigate().back();

        actions = new Actions(driver);
        actions.moveToElement(remove).click().perform();
        assertTrue(driver.getCurrentUrl().contains("remove-pages"));
        driver.navigate().back();

        actions = new Actions(driver);
        actions.moveToElement(redact).click().perform();
        assertTrue(driver.getCurrentUrl().contains("auto-redact"));
    }

    @Test
    void ensureDarkAndLightModeWork() {
        WebElement toggle = mainPage.getDarkModeToggle();

        Actions actions = new Actions(driver);
        actions.moveToElement(toggle).click().perform();

        // dark mode
        WebElement textOnPage = mainPage.getTagline();
        assertTrue(textOnPage.getCssValue("color").contains("255, 255, 255"));

        actions = new Actions(driver);
        actions.moveToElement(toggle).click().perform();

        // light mode
        assertTrue(textOnPage.getCssValue("color").contains("33"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // PDF TO IMAGE TESTS //
    // again, the purpose of each test is explained by the name of the test


    // TEST REVEALING PDF TO IMAGE INTEGRATION ISSUE //
    // When a pdf is submitted to be converted to an image, an error message is shown. This happens regardless of the type.
    // This is an issue with the integration of the PdfToImage functionality with the UI.
    // This is a fault because the intended behavior is for the pdf to be converted to an image, not for an error message to be shown.
    @Test
    @Tag("Fault")
    void testPdfToImageIntegrationTIFF() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement inp = pdfToImagePage.getFileInput();

        inp.sendKeys("/Users/nader/Downloads/tennis_skills_main.pdf");

        WebElement selectType = pdfToImagePage.getImageTypeSelector();

        Select s = new Select(selectType);
        s.selectByVisibleText("TIFF");

        WebElement submitBtn = pdfToImagePage.getSubmitButton();

        actions = new Actions(driver);
        actions.moveToElement(submitBtn).click().perform();

        WebElement error = pdfToImagePage.getErrorContainer();

        // verify that the error banner is not shown
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions.visibilityOf(error));
            // if the element appears, an error was thrown
            fail();
        } catch (Exception ignored) {

        }
    }


    // TEST REVEALING PDF TO IMAGE UI ISSUE //
    // When the error message is closed and the submission is re-attempted, the page just stalls and the button says
    // "processing...." forever. This is a fault because the intended functionality is for the error message to be shown again.
    // The fact that it stalls at “processing...” misleads the user into thinking the process is working.
    @Test
    @Tag("Fault")
    void testPdfToImageSubmitThenCloseErrorThenSubmitAgainTIFF() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement inp = pdfToImagePage.getFileInput();

        inp.sendKeys("/Users/nader/Downloads/tennis_skills_main.pdf");

        WebElement selectType = pdfToImagePage.getImageTypeSelector();

        Select s = new Select(selectType);
        s.selectByVisibleText("TIFF");

        WebElement submitBtn = pdfToImagePage.getSubmitButton();

        actions = new Actions(driver);
        actions.moveToElement(submitBtn).click().perform();

        WebElement error = pdfToImagePage.getErrorContainer();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        wait.until(ExpectedConditions.visibilityOf(error));

        actions = new Actions(driver);
        actions.moveToElement(pdfToImagePage.getErrorClose()).click().perform();

        actions = new Actions(driver);
        actions.moveToElement(submitBtn).click().perform();

        // error message should re-appear. If it does not, this throws an exception
        wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        wait.until(ExpectedConditions.visibilityOf(error));
    }
    
    @Test
    void ensureToolbarLogo() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();


        WebElement w = pdfToImagePage.getLogo();

        assertTrue(w.isDisplayed());
    }

    @Test
    void ensureLogoGoesBackToHomeFromSubpage() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();


        WebElement w = pdfToImagePage.getLogo();

        actions = new Actions(driver);
        actions.moveToElement(w).click().perform();

        assertEquals(base, driver.getCurrentUrl());
    }

    @Test
    void ensureFavoritesStayFromHomeToSubpage() {
        WebElement card = mainPage.getRemoveCard();
        Actions actions = new Actions(driver);
        actions.moveToElement(card).perform();

        WebElement favoriteButton = driver.findElement(By.xpath("/html/body/div/div/div[3]/div/div[17]/div"));
        actions = new Actions(driver);
        actions.moveToElement(favoriteButton).click().perform();


        card = mainPage.getPdfToImageCard();

        actions = new Actions(driver);
        actions.moveToElement(card).click().perform();


        WebElement favDropdown = pdfToImagePage.getFavoritesDropdown();
        actions = new Actions(driver);
        actions.moveToElement(favDropdown).click().perform();

        WebElement container = pdfToImagePage.getFavoritesMenu();

        List<WebElement> children = container.findElements(By.xpath("./*"));

        // dropdown should only have 1 tool
        assertEquals(1, children.size());
        // and that tool should be the "remove" tool
        actions = new Actions(driver);
        actions.moveToElement(children.getFirst()).click().perform();
        assertTrue(driver.getCurrentUrl().contains("remove-pages"));
    }



    @Test
    void ensureDarkModeToggleOnSubpage() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();


        WebElement toggle = pdfToImagePage.getDarkModeToggle();

        actions = new Actions(driver);
        actions.moveToElement(toggle).click().perform();

        // dark mode
        WebElement textOnPage = pdfToImagePage.getPageHeadline();
        assertTrue(textOnPage.getCssValue("color").contains("255, 255, 255"));

        actions = new Actions(driver);
        actions.moveToElement(toggle).click().perform();

        // light mode
        assertTrue(textOnPage.getCssValue("color").contains("33"));
    }


    @Test
    void ensureSubPageHeadlineIsShown() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement headline = pdfToImagePage.getPageHeadline();

        assertTrue(headline.isDisplayed());
        assertTrue(headline.getText().contains("PDF to Image"));
    }

    @Test
    void ensureWarningAboutFileSizesIsShown() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement warning = pdfToImagePage.getPageWarning();

        assertTrue(warning.isDisplayed());
        assertTrue(warning.getText().contains("Warning: This process can take up to a minute depending on file-size"));
    }


    @Test
    void ensureFileInputExistsAndIsOfTypeFileInputAndOnlyAcceptsPDFs() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement fileInput = pdfToImagePage.getFileInput();

        assertTrue(fileInput.isDisplayed());
        assertEquals("file", fileInput.getAttribute("type"));
        assertEquals("application/pdf", fileInput.getAttribute("accept"));
    }


    @Test
    void ensureFileTypeSelectorExistsAndHasAppropriateSelectValues() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement typeSelect = pdfToImagePage.getImageTypeSelector();

        assertTrue(typeSelect.isDisplayed());
        List<WebElement> options = typeSelect.findElements(By.tagName("option"));

        assertEquals(5, options.size());
        assertEquals("PNG", options.get(0).getText());
        assertEquals("JPG", options.get(1).getText());
        assertEquals("GIF", options.get(2).getText());
        assertEquals("TIFF", options.get(3).getText());
        assertEquals("BMP", options.get(4).getText());
    }

    @Test
    void ensureResultingImageSelectorExistsAndHasAppropriateSelectValues() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement typeSelect = pdfToImagePage.getImageResultSelector();

        assertTrue(typeSelect.isDisplayed());
        List<WebElement> options = typeSelect.findElements(By.tagName("option"));

        assertEquals(2, options.size());
        assertEquals("Multiple Images", options.get(0).getText());
        assertEquals("Single Big Image", options.get(1).getText());
    }


    @Test
    void ensureResultingImageColorSelectorExistsAndHasAppropriateSelectValues() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement typeSelect = pdfToImagePage.getResultingImageColorSelector();

        assertTrue(typeSelect.isDisplayed());
        List<WebElement> options = typeSelect.findElements(By.tagName("option"));

        assertEquals(3, options.size());
        assertEquals("Color", options.get(0).getText());
        assertEquals("Grayscale", options.get(1).getText());
        assertEquals("Black and White (May lose data!)", options.get(2).getText());
    }

    @Test
    void ensureResultingImageDpiSelectorExistsAndIsNumericalInput() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement dpiSelector = pdfToImagePage.getResultingImageDpiSelector();
        assertTrue(dpiSelector.isDisplayed());
        System.out.println(dpiSelector.getAttribute("type"));
        assertTrue(dpiSelector.isDisplayed());
        assertEquals("number", dpiSelector.getAttribute("type"));
    }

    @Test
    void ensureConvertingEmptyInputDoesNotRun() {
        WebElement card = mainPage.getPdfToImageCard();

        Actions actions = new Actions(driver);
        actions.moveToElement(card).click().perform();

        WebElement submitButton = pdfToImagePage.getSubmitButton();
        actions = new Actions(driver);
        actions.moveToElement(submitButton).click().perform();

        assertTrue(driver.getCurrentUrl().contains("pdf-to-img"));
        assertFalse(submitButton.getText().contains("Processing.."));
    }
}
