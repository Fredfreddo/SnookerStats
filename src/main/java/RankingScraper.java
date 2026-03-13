import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class RankingScraper {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 10);

        String url = "https://cuetracker.net/rankings/2022-2023";
        String csvFilePath = "rankings_22_23.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
            // Updated CSV Header to match your requested order
            writer.println("Name,Country,Rank");
            System.out.println("Navigating to: " + url);
            driver.get(url);

            int pageNumber = 1;

            while (true) {
                System.out.println("Scraping page " + pageNumber + "...");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody")));

                List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));

                for (WebElement row : rows) {
                    List<WebElement> columns = row.findElements(By.tagName("td"));

                    if (columns.size() >= 2) {
                        // 1. Get Rank
                        String rank = columns.get(0).getText().trim().replace("=", "");

                        // 2. Get Name (getText() safely ignores the image element itself)
                        String name = columns.get(1).getText().trim();

                        // 3. Get Country
                        String country = "Unknown";
                        try {
                            // Find the flag image within this specific row
                            WebElement flagImg = row.findElement(By.tagName("img"));
                            country = flagImg.getAttribute("title");

                            // Fallback just in case the website uses 'alt' instead of 'title'
                            if (country == null || country.isEmpty()) {
                                country = flagImg.getAttribute("alt");
                            }
                        } catch (org.openqa.selenium.NoSuchElementException e) {
                            // If no flag image is found, check if it's stored as plain text in a 3rd column
                            if (columns.size() > 2) {
                                country = columns.get(2).getText().trim();
                            }
                        }

                        // Write to CSV: Name, Country, Rank
                        // Wrapped Name and Country in quotes to prevent CSV breakage if they contain commas
                        writer.printf("\"%s\",\"%s\",%s%n", name, country, rank);
                    }
                }

                try {
                    WebElement nextButton = driver.findElement(By.xpath("//ul[contains(@class, 'pagination')]//a[contains(text(), 'Next')]"));

                    WebElement parentLi = nextButton.findElement(By.xpath(".."));
                    if (parentLi.getAttribute("class").contains("disabled")) {
                        System.out.println("Reached the last page.");
                        break;
                    }

                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
                    nextButton.click();
                    Thread.sleep(1500);
                    pageNumber++;

                } catch (org.openqa.selenium.NoSuchElementException e) {
                    System.out.println("No more pages found.");
                    break;
                }
            }

            System.out.println("Successfully saved rankings to " + csvFilePath);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}