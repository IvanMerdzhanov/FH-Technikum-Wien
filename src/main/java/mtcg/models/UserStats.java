package mtcg.models;

public class UserStats {
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private int eloRating;

    // Constructor
    public UserStats(int totalWins, int totalLosses, int totalDraws, int eloRating) {
        this.totalWins = totalWins;
        this.totalLosses = totalLosses;
        this.totalDraws = totalDraws;
        this.eloRating = eloRating;
    }

    // Getters and Setters
    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
    }

    public int getTotalDraws() {
        return totalDraws;
    }

    public void setTotalDraws(int totalDraws) {
        this.totalDraws = totalDraws;
    }

    public int getEloRating() {
        return eloRating;
    }

    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }

    // Update methods for wins, losses, and draws
    public void recordWin() {
        this.totalWins++;
        updateEloRating(true);
    }

    public void recordLoss() {
        this.totalLosses++;
        updateEloRating(false);
    }

    // Method to update ELO rating
    private void updateEloRating(boolean isWin) {
        if (isWin) {
            this.eloRating += 100; // Example increment for a win
        } else {
            this.eloRating -= 100; // Example decrement for a loss
        }
        // Ensure ELO rating does not go below a certain threshold, e.g., 0
        this.eloRating = Math.max(this.eloRating, 0);
    }
    public void incrementWins(int wins) {
        this.totalWins += wins;
    }

    public void incrementLosses(int losses) {
        this.totalLosses += losses;
    }

    public void incrementDraws(int draws) {
        this.totalDraws += draws;
    }
    public void adjustEloRating(int change) {
        this.eloRating += change;
    }

    public void incrementEloRating(int i) {
        eloRating += i;
    }
    public void decrementEloRating(int i){
        eloRating -= i;
    }
}

