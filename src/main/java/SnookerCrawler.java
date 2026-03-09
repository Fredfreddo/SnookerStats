import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.*;
import java.util.stream.Collectors;

public class SnookerCrawler {
    private WebDriver driver;
    private WebDriverWait wait;
    private Set<Player> players;

    public SnookerCrawler() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, 10);
        players = new HashSet<>();
    }

    public void endCrawl() {
        if (driver != null) {
            driver.quit();
        }
    }

    public Season getSeason(String urlCuetracker){
        Season season = new Season();
        season.setCuetrackerURL(urlCuetracker);
        season.setSeason(urlCuetracker.substring(urlCuetracker.length()-9, urlCuetracker.length()));
        try {
            driver.get(urlCuetracker);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
            List<WebElement> tournamentAnchors = driver.findElements(
                    By.xpath("//a[contains(@href, '/tournaments/')]"));
            for  (WebElement anchor : tournamentAnchors) {
                String tournamentURL = anchor.getAttribute("href");
                String tournamentName = anchor.getText();
                Tournament tournament = new Tournament();
                tournament.setName(tournamentName);
                tournament.setCuetrackerURL(tournamentURL);
                season.addTournament(tournament);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return season;
    }

    public void processTournament(Season season){
        List<Tournament> tournaments = season.getTournaments();
        int testCounter = 0;
        for  (Tournament tournament : tournaments) {
            if (testCounter++ > 0) {
                break;
            }
            try{
                String tournamentURL = tournament.getCuetrackerURL();
                driver.get(tournamentURL);
                List<WebElement> matchContainers =
                        driver.findElements(
                                By.xpath("//div[starts-with(@id, 'round') and contains(@id, 'match')]"));
                for (WebElement container : matchContainers) {
                    Match match = new Match();

                    WebElement p1Element = container.findElement(By.xpath(".//div[1]/div[2]/div[1]"));
                    WebElement p2Element = container.findElement(By.xpath(".//div[1]/div[2]/div[3]"));
                    String player1 = p1Element.getText().trim();
                    String player2 = p2Element.getText().trim();
                    match.setPlayer1(player1);
                    match.setPlayer2(player2);

                    WebElement score1Element = container.findElement(By.xpath(".//div[1]/div[2]/div[2]/span[1]"));
                    WebElement bestOfFrames = container.findElement(By.xpath(".//div[1]/div[2]/div[2]/span[2]"));
                    WebElement score2Element = container.findElement(By.xpath(".//div[1]/div[2]/div[2]/span[3]"));
                    int socre1 = Integer.parseInt(score1Element.getText().trim());
                    int socre2 = Integer.parseInt(score2Element.getText().trim());
                    String bestOfText = bestOfFrames.getText().trim();
                    bestOfText = bestOfText.substring(1, bestOfText.length() - 1);
                    int bestOf = Integer.parseInt(bestOfText);
                    match.setPlayer1Score(socre1);
                    match.setPlayer2Score(socre2);
                    match.setBestOfFrames(bestOf);


                    tournament.addMatch(match);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SnookerCrawler crawler = new SnookerCrawler();
        Season season = crawler.getSeason("https://cuetracker.net/seasons/2025-2026");
        crawler.processTournament(season);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //String json = gson.toJson(season);
        //System.out.println(json);
        // save gson to a json file
        try (Writer writer = new FileWriter("season_2025_2026.json")) {
            gson.toJson(season, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        crawler.endCrawl();
    }

}
