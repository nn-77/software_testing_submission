import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class MainPageObjectModel {

    private WebDriver driver;

    public MainPageObjectModel(WebDriver driver) {
        this.driver = driver;
    }

    public WebElement getLogo() {
        return driver.findElement(By.xpath("//a[@class='navbar-brand']"));
    }

    public WebElement getGithubLink() {
        return driver.findElement(By.xpath("//a[contains(@title, 'Visit')]"));
    }

    public WebElement getDockerLink() {
        return driver.findElement(By.xpath("//img[@alt='docker']"));
    }

    public WebElement getDiscordLink() {
        return driver.findElement(By.xpath("//img[@alt='discord']"));
    }

    public WebElement getSponsorLink() {
        return driver.findElement(By.xpath("//img[@alt='suit-heart-fill']"));
    }

    public WebElement getLicensesLink() {
        return driver.findElement(By.xpath("//*[@id='licenses']"));
    }


    public WebElement getMainDivContainer() {
        return driver.findElement(By.xpath("//div[@class=' container']"));
    }

    public WebElement getTagline() {
        return driver.findElement(By.xpath("/html/body/div/div/div[2]/div/p"));
    }

    public WebElement getPageOperationsDropdown_remove() {
        WebElement w = driver.findElement(By.xpath("//*[@id='navbarDropdown-1']"));
        Actions actions = new Actions(driver);
        actions.moveToElement(w).click().perform();

        return driver.findElement(By.xpath("/html/body/div/div/div[1]/nav/div/div/ul[1]/li[5]/div/a[5]/span"));
    }

    public WebElement getSecurityDropdown_autoredact() {
        WebElement w = driver.findElement(By.xpath("//*[@id='navbarDropdown-3']"));
        Actions actions = new Actions(driver);
        actions.moveToElement(w).click().perform();

        return driver.findElement(By.xpath("/html/body/div/div/div[1]/nav/div/div/ul[1]/li[9]/div/a[7]/span"));
    }

    public WebElement getMiscellaneousDropdown_view() {
        WebElement w = driver.findElement(By.xpath("//*[@id='navbarDropdown-4']"));
        Actions actions = new Actions(driver);
        actions.moveToElement(w).click().perform();

        return driver.findElement(By.xpath("/html/body/div/div/div[1]/nav/div/div/ul[1]/li[11]/div/a[1]/span"));
    }

    public WebElement getRemoveCard() {
        return driver.findElement(By.xpath("//*[@id='remove-pages']"));
    }

    public WebElement getViewCard() {
        return driver.findElement(By.xpath("//*[@id='view-pdf']"));
    }

    public WebElement getRedactCard() {
        return driver.findElement(By.xpath("//*[@id='auto-redact']"));
    }

    public WebElement getToolCardContainer() {
        return driver.findElement(By.xpath("//div[@class='features-container']"));
    }

    public WebElement getFavoritesDropdownToolbar() {
        return driver.findElement(By.xpath("//*[@id='navbarDropdown-5']"));
    }

    public WebElement getFavoritesDropdownContentList() {
        return driver.findElement(By.xpath("//*[@id='favoritesDropdown']"));
    }

    public WebElement getDarkModeToggle() {
        return driver.findElement(By.xpath("//*[@id='dark-mode-toggle']"));
    }

    public WebElement getPdfToImageCard() {
        return driver.findElement(By.xpath("//*[@id='pdf-to-img']"));
    }
}
