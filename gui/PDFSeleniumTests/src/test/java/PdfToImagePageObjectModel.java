import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class PdfToImagePageObjectModel {
    private WebDriver driver;

    public PdfToImagePageObjectModel(WebDriver driver) {
        this.driver = driver;
    }


    public WebElement getFileInput() {
        return driver.findElement(By.xpath("//*[@id='fileInput-input']"));
    }

    public WebElement getImageTypeSelector() {
        return driver.findElement(By.xpath("//select[@name='imageFormat']"));
    }

    public WebElement getSubmitButton() {
        return driver.findElement(By.xpath("//*[@id='submitBtn']"));
    }

    public WebElement getErrorContainer() {
        return driver.findElement(By.xpath("//*[@id='errorContainer']"));
    }

    public WebElement getErrorClose() {
        return driver.findElement(By.xpath("//button[@data-bs-dismiss='alert']"));
    }

    public WebElement getLogo() {
        return driver.findElement(By.xpath("//img[@class='main-icon']"));
    }

    public WebElement getFavoritesDropdown() {
        return driver.findElement(By.xpath("//*[@id='navbarDropdown-5']"));
    }

    public WebElement getFavoritesMenu() {
        return driver.findElement(By.xpath("//*[@id='favoritesDropdown']"));
    }

    public WebElement getDarkModeToggle() {
        return driver.findElement(By.xpath("//*[@id='dark-mode-toggle']"));
    }

    public WebElement getPageHeadline() {
        return driver.findElement(By.xpath("/html/body/div/div/div[2]/div/div/h2"));
    }

    public WebElement getPageWarning() {
        return driver.findElement(By.xpath("/html/body/div/div/div[2]/div/div/p"));
    }

    public WebElement getImageResultSelector() {
        return driver.findElement(By.xpath("//select[@name='singleOrMultiple']"));
    }

    public WebElement getResultingImageColorSelector() {
        return driver.findElement(By.xpath("//select[@name='colorType']"));
    }

    public WebElement getResultingImageDpiSelector() {
        return driver.findElement(By.xpath("//*[@id='dpi']"));
    }
}
