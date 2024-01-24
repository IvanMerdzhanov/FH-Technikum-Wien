package mtcg.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Battle {
    private User playerOne;
    private User playerTwo;
    private Random random;
    private List<String> roundResults;

    public Battle(User playerOne, User playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.random = new Random();
        this.roundResults = new ArrayList<>();
    }

    public String startBattle() {
        if (playerOne.getDeck().isEmpty() || playerTwo.getDeck().isEmpty()) {
            return "Battle cannot start: One of the players has an empty deck.";
        }
        System.out.println("Battle started between " + playerOne.getUsername() + " and " + playerTwo.getUsername());
        StringBuilder battleSummary = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            System.out.println("Round " + (i + 1) + " starts");
            Card cardOne = getRandomCardFromDeck(playerOne.getDeck());
            Card cardTwo = getRandomCardFromDeck(playerTwo.getDeck());

            if (cardOne == null || cardTwo == null) {
                System.out.println("Ending battle as one of the players ran out of cards.");
                break;
            }

            playerOne.getDeck().removeCard(cardOne);
            playerTwo.getDeck().removeCard(cardTwo);

            Round round = new Round(playerOne, cardOne, playerTwo, cardTwo);
            List<String> roundResultsList = round.executeRound();
            for (String detail : roundResultsList) {
                System.out.println(detail);
                roundResults.add(detail);
                battleSummary.append(detail);
            }
        }

        String winner = determineWinner();
        System.out.println("Overall Battle Winner: " + winner);
        return winner;
    }

    private void updateStats(User user, Connection conn, int win, int loss, int draw) throws SQLException {
        System.out.println("!!!Entering updateStats!!!");
        UserStats stats = user.getUserStats();
        stats.incrementWins(win);
        stats.incrementLosses(loss);
        stats.incrementDraws(draw);

        // Update ELO rating
        if (win == 1) {
            stats.incrementEloRating(3);
            System.out.println("Incrementing ELO by 3 for win.");
        } else if (loss == 1) {
            stats.decrementEloRating(5);
            System.out.println("Decrementing ELO by 5 for loss.");
        }

        // Update in database
        String sql = "UPDATE user_statistics SET total_wins = ?, total_losses = ?, total_draws = ?, elo_rating = ? WHERE username = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(sql)) {
            updateStmt.setInt(1, stats.getTotalWins());
            updateStmt.setInt(2, stats.getTotalLosses());
            updateStmt.setInt(3, stats.getTotalDraws());
            updateStmt.setInt(4, stats.getEloRating());
            updateStmt.setString(5, user.getUsername());
            updateStmt.executeUpdate();
            System.out.println("Stats updated for user: " + user.getUsername() +
                    " Wins: " + stats.getTotalWins() +
                    " Losses: " + stats.getTotalLosses() +
                    " Draws: " + stats.getTotalDraws() +
                    " ELO: " + stats.getEloRating());
            System.out.println("!!!Exeting updateStats!!!");
        }
    }


    public String determineWinner() {
        System.out.println("!!!Entering determineWinner!!!");
        // Logic to determine the winner based on the results
        int winsPlayerOne = 0, winsPlayerTwo = 0;
        for (String result : roundResults) {
            if (result.contains("Player 1 wins")) {
                winsPlayerOne++;
            } else if (result.contains("Player 2 wins")) {
                winsPlayerTwo++;
            }
        }

        if (winsPlayerOne > winsPlayerTwo) {
            System.out.println("PlayerOne Wins");
            System.out.println("!!!Exeting determineWinner!!!");
            return playerOne.getUsername();
        } else if (winsPlayerTwo > winsPlayerOne) {
            System.out.println("PlayerTwo Wins");
            System.out.println("!!!Exeting determineWinner!!!");
            return playerTwo.getUsername();
        } else {
            System.out.println("Draw");
            System.out.println("!!!Exeting determineWinner!!!");
            return "Draw";
        }
    }

    public void finishBattle(String winner, Connection conn) throws SQLException {
        int winsPlayerOne = 0, winsPlayerTwo = 0;
        // Calculate wins for each player
        for (String result : roundResults) {
            if (result.contains("Player 1 wins")) {
                winsPlayerOne++;
            } else if (result.contains("Player 2 wins")) {
                winsPlayerTwo++;
            }
        }

        // Update stats based on the winner
        if ("Draw".equals(winner)) {
            updateStats(playerOne, conn, 0, 0, 1);
            updateStats(playerTwo, conn, 0, 0, 1);
        } else if (playerOne.getUsername().equals(winner)) {
            updateStats(playerOne, conn, 1, 0, 0);
            updateStats(playerTwo, conn, 0, 1, 0);
        } else {
            updateStats(playerOne, conn, 0, 1, 0);
            updateStats(playerTwo, conn, 1, 0, 0);
        }
    }

    private Card getRandomCardFromDeck(Deck deck) {
        if (deck.getCards().isEmpty()) {
            return null; // Handle case where the deck has no cards
        }
        return deck.getCards().get(random.nextInt(deck.getCards().size()));
    }

    public List<String> getRoundResults() {
        return roundResults;
    }
}