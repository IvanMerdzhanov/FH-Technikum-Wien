package mtcg.models;

import java.util.List;

public class BattleOutcome {
    private String winner;
    private List<String> roundResults;

    public BattleOutcome(String winner, List<String> roundResults) {
        this.winner = winner;
        this.roundResults = roundResults;
    }

    public String getWinner() {
        return winner;
    }

    public List<String> getRoundResults() {
        return roundResults;
    }
}
