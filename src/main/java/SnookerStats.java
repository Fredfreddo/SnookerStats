import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.stream.Collectors;

public class SnookerStats {

    public static void main(String[] args) {
        // Set up ChromeDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless"); // Run in headless mode to speed up execution
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 10);

        try {
            // 1. Get Top 32 Players from snooker.org
            System.out.println("Fetching Top 32 players from snooker.org...");
            Set<String> top32Players = getTop32Players(driver, wait);
            System.out.println("Top 32 Players identified: " + top32Players);

            // 2. Get Tournament Links from cuetracker.net
            System.out.println("\nFetching tournament links from cuetracker.net...");
            List<String> tournamentLinks = getTournamentLinks(driver, wait);
            System.out.println("Found " + tournamentLinks.size() + " valid tournaments.");

            // 3. Process Matches and Track Stats
            Map<String, PlayerStats> playerStatsMap = new HashMap<>();
            for (String player : top32Players) {
                playerStatsMap.put(player, new PlayerStats(player));
            }

            System.out.println("\nProcessing matches... (This may take a while)");
            for (String tournamentLink : tournamentLinks) {
                processTournament(driver, tournamentLink, top32Players, playerStatsMap);
            }

            // 4. Calculate Win Rates and Sort
            List<PlayerStats> sortedStats = playerStatsMap.values().stream()
                    .filter(stats -> stats.getTotalMatches() > 0) // Only include players who played
                    .sorted((p1, p2) -> Double.compare(p2.getWinRate(), p1.getWinRate()))
                    .collect(Collectors.toList());

            // 5. Output the Table and Save to CSV
            String csvFilePath = "top32_win_rates.csv"; // You can change the file name/path here

            System.out.println("\n=======================================================");
            System.out.printf("%-25s | %-10s | %-10s | %-10s\n", "Player Name", "Matches", "Wins", "Win Rate");
            System.out.println("=======================================================");

            // Using try-with-resources to ensure the writer closes automatically
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(csvFilePath))) {

                // Write the CSV header
                writer.println("Player Name,Matches,Wins,Win Rate");

                for (PlayerStats stats : sortedStats) {
                    double winRatePercentage = stats.getWinRate() * 100;

                    // 1. Print to the console (keeping your original formatting)
                    System.out.printf("%-25s | %-10d | %-10d | %-6.2f%%\n",
                            stats.getName(), stats.getTotalMatches(), stats.getWins(), winRatePercentage);

                    // 2. Write to the CSV file (comma separated, no extra padding spaces)
                    writer.printf("%s,%d,%d,%.2f%%\n",
                            stats.getName(), stats.getTotalMatches(), stats.getWins(), winRatePercentage);
                }
                System.out.println("=======================================================");
                System.out.println("Success! Data also saved to: " + csvFilePath);
            } catch (java.io.IOException e) {
                System.err.println("Error writing to CSV file: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Set<String> getTop32Players(WebDriver driver, WebDriverWait wait) {
        driver.get("https://www.snooker.org/res/index.asp?template=33&season=2025");
        Set<String> players = new HashSet<>();

        // Use this robust XPath approach:
        // 1. Find the specific table that contains a table header (th) with the exact text "Player"
        String robustTableXPath = "//table[.//thead[@id='first']]";
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(robustTableXPath)));

        // 2. Fetch the rows specifically from that table
        List<WebElement> rows = driver.findElements(By.xpath(robustTableXPath + "/tbody/tr"));

        int count = 0;
        for (WebElement row : rows) {
            if (count >= 32) break;

            try {
                // Adjust selector based on snooker.org exact DOM, usually the second/third column holds the player link
                WebElement playerLink = row.findElement(By.cssSelector("a"));
                String playerName = normalizeName(playerLink.getText().trim());
                if (!playerName.isEmpty()) {
                    players.add(playerName);
                    count++;
                }
            } catch (Exception ignored) {
                // Ignore rows that don't match the format (e.g., headers)
            }
        }
        players.add("mark williams");
        return players;
    }

    private static List<String> getTournamentLinks(WebDriver driver, WebDriverWait wait) {
        driver.get("https://cuetracker.net/seasons/2025-2026");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));

        List<String> links = new ArrayList<>();
        List<WebElement> tournamentAnchors = driver.findElements(By.xpath("//a[contains(@href, '/tournaments/')]"));

        for (WebElement anchor : tournamentAnchors) {
            String link = anchor.getAttribute("href");
            String text = anchor.getText().trim();

            // Skip Championship League and Q School
            if (!text.contains("Championship League") && link != null && !text.contains("Q School")) {
                // Avoid duplicates
                if (!links.contains(link)) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    private static void processTournament(WebDriver driver, String url, Set<String> top32Players, Map<String, PlayerStats> statsMap) {
        driver.get(url);
        try {
            // Find all match container divs based on the ID pattern you discovered
            // This selects any div where the ID starts with 'round' and contains 'match'
            List<WebElement> matchContainers = driver.findElements(By.xpath("//div[starts-with(@id, 'round') and contains(@id, 'match')]"));

            for (WebElement container : matchContainers) {
                try {
                    // Extract players using the relative XPath structure from the container
                    WebElement p1Element = container.findElement(By.xpath(".//div[1]/div[2]/div[1]"));
                    WebElement p2Element = container.findElement(By.xpath(".//div[1]/div[2]/div[3]"));

                    String p1 = normalizeName(p1Element.getText().trim());
                    String p2 = normalizeName(p2Element.getText().trim());

                    // Check if both players are in the top 32
                    if (top32Players.contains(p1) && top32Players.contains(p2)) {

                        // Extract scores
                        WebElement score1Element = container.findElement(By.xpath(".//div[1]/div[2]/div[2]/span[1]"));
                        WebElement score2Element = container.findElement(By.xpath(".//div[1]/div[2]/div[2]/span[3]"));

                        String score1Text = score1Element.getText().trim();
                        String score2Text = score2Element.getText().trim();

                        try {
                            // Parse scores to integers
                            int score1 = Integer.parseInt(score1Text);
                            int score2 = Integer.parseInt(score2Text);

                            // Record the match played
                            statsMap.get(p1).addMatch();
                            statsMap.get(p2).addMatch();

                            // Determine winner
                            if (score1 > score2) {
                                statsMap.get(p1).addWin();
                            } else if (score2 > score1) {
                                statsMap.get(p2).addWin();
                            }
                        } catch (NumberFormatException e) {
                            // This gracefully handles matches that weren't played (e.g., "W/O" for walkovers, "v" for upcoming)
                            // We just skip adding the match/win to the stats.
                        }
                    }
                } catch (Exception innerE) {
                    // If a specific match div is missing a player or score element (e.g., a bye round),
                    // this catches the error so the loop can safely continue to the next match.
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing tournament at URL: " + url);
        }
    }

    // Helper method to normalize names across different websites (e.g., removing middle initials, handling accents)
    private static String normalizeName(String name) {
        // Convert to lowercase for easier matching, remove extra spaces
        return name.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    // Helper Class to store player statistics
    static class PlayerStats {
        private String name;
        private int wins;
        private int totalMatches;

        public PlayerStats(String name) {
            this.name = name;
            this.wins = 0;
            this.totalMatches = 0;
        }

        public String getName() {
            // Capitalize names for display purposes
            return Arrays.stream(name.split(" "))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));
        }

        public void addMatch() {
            this.totalMatches++;
        }

        public void addWin() {
            this.wins++;
        }

        public int getTotalMatches() {
            return totalMatches;
        }

        public int getWins() {
            return wins;
        }

        public double getWinRate() {
            if (totalMatches == 0) return 0.0;
            return (double) wins / totalMatches;
        }
    }
}