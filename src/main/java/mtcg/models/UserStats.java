package mtcg.models;

public class UserStats {
    private int coins;
    private int eloScore;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;


    public UserStats() {
        this.coins = 20; // Every user starts with 20 coins
        this.eloScore = 100; // Starting ELO score is 100
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
    }

    public void deductCoins(int coins) {
        if(this.coins - coins < 0) {
            System.out.println("Insufficient coins.");
            return;
        }
        this.coins -= coins;
    }

    public boolean updateEloScore(boolean win) {
        // Let's assume you don't want the ELO score to go below zero
        if (win) {
            this.eloScore += 3;
            this.gamesWon++;
        } else {
            // Check if the ELO score would go below zero after losing
            if (this.eloScore - 5 < 0) {
                // If it does, you might decide not to change the score and return false
                return false; // Update not successful because it would result in a negative score
            }
            this.eloScore -= 5;
            this.gamesLost++;
        }
        this.gamesPlayed++;
        return true; // Update successful
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getEloScore() {
        return eloScore;
    }

    public void setEloScore(int eloScore) {
        this.eloScore = eloScore;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }
}
