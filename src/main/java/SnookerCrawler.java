import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Random;

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
        // print out got season
        System.out.println("Processing Season: " + season.getSeason());
        try {
            driver.get(urlCuetracker);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
            List<WebElement> tournamentAnchors = driver.findElements(
                    By.xpath("//a[contains(@href, '/tournaments/')]"));
            for  (WebElement anchor : tournamentAnchors) {
                String tournamentURL = anchor.getAttribute("href");
                String tournamentName = anchor.getText();
                if (!tournamentName.contains("Q School") && !tournamentName.contains("6-Reds")){
                    Tournament tournament = new Tournament();
                    tournament.setName(tournamentName);
                    tournament.setCuetrackerURL(tournamentURL);
                    season.addTournament(tournament);
                }
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
        Random random = new Random();
        for  (Tournament tournament : tournaments) {
//            if (testCounter++ > 0) {
//                break;
//            }
            // print out processing tournament
            System.out.println("Processing Tournament: " + tournament.getName());
            try {
                // Wait for a minimum of 1+ seconds
                int minWait = 1000;
                int randomWait = random.nextInt(2000);
                int totalWait = minWait + randomWait;

                System.out.println("Waiting for " + (totalWait / 1000.0) + " seconds...");
                Thread.sleep(totalWait);

            } catch (InterruptedException e) {
                // This catches the error if the sleep is interrupted
                Thread.currentThread().interrupt();
                System.out.println("Sleep interrupted!");
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
                    if (player1.contains("Walkover")){
                        continue;
                    }
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

                    WebElement dateElement = container.findElement(
                            By.xpath(".//div[1]/div[4]/div[2]/div[1]/div[1]"));
                    match.setDate(dateElement.getText().trim());

                    WebElement player1BreaksElement = container.findElement(
                            By.xpath(".//div[contains(text(), '50+ Breaks')]/following-sibling::div//div[contains(@class, 'col-4')][1]"));
                    WebElement player2BreaksElement = container.findElement(
                            By.xpath(".//div[contains(text(), '50+ Breaks')]/following-sibling::div//div[contains(@class, 'col-4')][2]"));
                    // breaks are string like "50, 70, 90". make them into list of integers
                    String player1BreaksText = player1BreaksElement.getText().trim();
                    String player2BreaksText = player2BreaksElement.getText().trim();
                    List<Integer> player1Breaks = Arrays.stream(player1BreaksText.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    List<Integer> player2Breaks = Arrays.stream(player2BreaksText.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    match.setBreaksPlayer1(player1Breaks);
                    match.setBreaksPlayer2(player2Breaks);

                    WebElement player1CountryElement = container.findElement(
                            By.xpath(".//div[1]/div[2]/div[1]//img"));
                    WebElement player2CountryElement = container.findElement(
                            By.xpath(".//div[1]/div[2]/div[3]//img"));
                    String player1Country = player1CountryElement.getAttribute("alt").trim();
                    String player2Country = player2CountryElement.getAttribute("alt").trim();
                    match.setPlayer1Country(player1Country);
                    match.setPlayer2Country(player2Country);

                    WebElement roundElement = container.findElement(
                            By.xpath(".//div[1]//h5"));
                    match.setRound(roundElement.getText().trim());


                    tournament.addMatch(match);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getDataFromSeason(SnookerCrawler crawler, String urlCuetracker) {
        Season season = crawler.getSeason(urlCuetracker);
        crawler.processTournament(season);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String fileName = "season_" + season.getSeason() + ".json";
        try (Writer writer = new FileWriter(fileName)) {
            gson.toJson(season, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        crawler.endCrawl();
    }

    public static void main(String[] args) {
//        SnookerCrawler crawler3 = new SnookerCrawler();
//        String urlCuetracker3 = "https://cuetracker.net/seasons/2023-2024";
//        crawler3.getDataFromSeason(crawler3, urlCuetracker3);
//
//        SnookerCrawler crawler = new SnookerCrawler();
//        String urlCuetracker = "https://cuetracker.net/seasons/2024-2025";
//        crawler.getDataFromSeason(crawler, urlCuetracker);
//
//        SnookerCrawler crawler2 = new SnookerCrawler();
//        String urlCuetracker2 = "https://cuetracker.net/seasons/2025-2026";
//        crawler2.getDataFromSeason(crawler2, urlCuetracker2);

        SnookerCrawler crawler4 = new SnookerCrawler();
        String urlCuetracker4 = "https://cuetracker.net/seasons/2022-2023";
        crawler4.getDataFromSeason(crawler4, urlCuetracker4);


    }

}
