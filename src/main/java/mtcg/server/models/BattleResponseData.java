package mtcg.server.models;

import java.util.List;

public class BattleResponseData {
    private String winner;
    private List<String> roundDetails; // Detailed description of each round
    private String playerOneStats; // Post-battle stats of player one
    private String playerTwoStats; // Post-battle stats of player two

    // Constructors, getters, and setters

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public List<String> getRoundDetails() {
        return roundDetails;
    }

    public void setRoundDetails(List<String> roundDetails) {
        this.roundDetails = roundDetails;
    }

    public String getPlayerOneStats() {
        return playerOneStats;
    }

    public void setPlayerOneStats(String playerOneStats) {
        this.playerOneStats = playerOneStats;
    }

    public String getPlayerTwoStats() {
        return playerTwoStats;
    }

    public void setPlayerTwoStats(String playerTwoStats) {
        this.playerTwoStats = playerTwoStats;
    }
}
