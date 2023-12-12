package mtcg.server.models;

public class BattleRequestData {
    private String tokenPlayerOne;
    private String tokenPlayerTwo;

    // Getters and Setters
    public String getTokenPlayerOne() {
        return tokenPlayerOne;
    }

    public void setTokenPlayerOne(String tokenPlayerOne) {
        this.tokenPlayerOne = tokenPlayerOne;
    }

    public String getTokenPlayerTwo() {
        return tokenPlayerTwo;
    }

    public void setTokenPlayerTwo(String tokenPlayerTwo) {
        this.tokenPlayerTwo = tokenPlayerTwo;
    }
}
