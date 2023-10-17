package mtcg.models;

public class UserStats {
    private int coins;
    private int eloScore;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    // Other attributes related to user stats can be added here

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

    public void updateEloScore(boolean win) {
        if(win) {
            this.eloScore += 3;
            gamesWon++;
        } else {
            this.eloScore -= 5;
            gamesLost++;
        }
        gamesPlayed++;
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
