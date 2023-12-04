package mtcg.server.models;
import mtcg.models.User;
public class BattleRequestData {
    private User playerOne;
    private User playerTwo;

    public User getPlayerOne() {
        return playerOne;
    }

    public void setPlayerOne(User playerOne) {
        this.playerOne = playerOne;
    }

    public User getPlayerTwo() {
        return playerTwo;
    }

    public void setPlayerTwo(User playerTwo) {
        this.playerTwo = playerTwo;
    }
}
