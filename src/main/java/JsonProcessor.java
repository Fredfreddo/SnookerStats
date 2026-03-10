import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class JsonProcessor {
    private Map<String, Player> players = new HashMap<>();
    private static final double K_BASE = 32.0;

    public Map<String, Player> getPlayers() {
        return players;
    }


    // read json file (such as "season_2023-2024.json")
    // make a sorted list of matches by date, from oldest to newest,
    // return the list of matches
    public List<Match> readMatchesFromJson(String fileName) {
            Gson gson = new Gson();
            try (Reader reader = new FileReader(fileName)) {
                Season season = gson.fromJson(reader, Season.class);
                List<Match> matches = new ArrayList<>();
                int index = 0;
                for (Tournament tournament : season.getTournaments()) {
                    for  (Match match : tournament.getMatches()) {
                        match.setOriginalIndex(index);
                        matches.add(match);
                        index++;
                    }
                }
                // sort matches by date, from oldest to newest
                // if same date, maintain the reverse of original order
                // (so that the matches in the same tournament are in the same order as in the json file)
                matches.sort(Comparator.comparing(Match::getDate)
                        .thenComparing(Comparator.comparingInt(Match::getOriginalIndex).reversed()));
                return matches;
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
    }

    // process the sorted list of matches
    // for each match, if a player does not exist in the players set,
    // initialize a new player with the player's name and country using constructor,
    // and add the player to the players set
    public void processMatches(List<Match> matches) {
        for (Match match : matches) {
            String player1Name = match.getPlayer1();
            String player2Name = match.getPlayer2();
            String player1Country = match.getPlayer1Country();
            String player2Country = match.getPlayer2Country();

            if (!players.containsKey(player1Name)) {
                Player player1 = new Player(player1Name, player1Country);
                players.put(player1Name, player1);
            }
            if (!players.containsKey(player2Name)) {
                Player player2 = new Player(player2Name, player2Country);
                players.put(player2Name, player2);
            }

            // now get players' points
            Player player1 = players.get(player1Name);
            Player player2 = players.get(player2Name);
            double player1Points = player1.getCurrentFormPoints();
            double player2Points = player2.getCurrentFormPoints();
            int player1Score = match.getPlayer1Score();
            int player2Score = match.getPlayer2Score();
            int bestOfFrames = match.getBestOfFrames();

            // calculate expected score for player 1
            double expectedScorePlayer1 = 1.0 / (1.0 + Math.pow(10.0, (player2Points - player1Points) / 400.0));
            double expectedScorePlayer2 = 1.0 - expectedScorePlayer1;
            // actual score for player 1
            double actualResultPlayer1 = (player1Score > player2Score) ? 1.0 : (player1Score == player2Score) ? 0.5 : 0.0;
            double actualResultPlayer2 = 1.0 - actualResultPlayer1;
            // scale K by best of frames
            double kScaled = K_BASE * Math.sqrt(bestOfFrames / 11.0);
            // get points change for player 1 and player 2
            double pointsChangePlayer1 = kScaled * (actualResultPlayer1 - expectedScorePlayer1);
            double pointsChangePlayer2 = kScaled * (actualResultPlayer2 - expectedScorePlayer2);
            // update players' points
            player1.setCurrentFormPoints(player1Points + pointsChangePlayer1);
            player2.setCurrentFormPoints(player2Points + pointsChangePlayer2);
        }
    }

    public static void main(String[] args) {
        JsonProcessor processor = new JsonProcessor();
        List<Match> matches2023_2024 = processor.readMatchesFromJson("season_2023-2024.json");
        processor.processMatches(matches2023_2024);
        System.out.println("There are " + processor.getPlayers().size() + " unique players in the 2023-2024 season.");
        System.out.println("Players in 2023-2024 season:");
        // print out players and their points, sorted by points from highest to lowest
        processor.getPlayers().values().stream()
                .sorted(Comparator.comparingDouble(Player::getCurrentFormPoints).reversed())
                .forEach(player -> System.out.println(player.getName() + " (" + player.getCountry() + "): " + player.getCurrentFormPoints()));

    }
}
