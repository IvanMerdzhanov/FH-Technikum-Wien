package mtcg.models;


import java.util.List;
import java.util.Random;

public class Battle {
    private User playerOne;
    private User playerTwo;
    private Random random;

    public Battle(User playerOne, User playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.random = new Random();
    }

    public void startBattle() {
        // Example loop for multiple rounds (limit based on game rules)
        for (int i = 0; i < 1; i++) {
            Round round = new Round(playerOne, getRandomCardFromDeck(playerOne), playerTwo, getRandomCardFromDeck(playerTwo));
            round.executeRound();
            // Additional battle logic (e.g., handling round results, updating stats)
        }
    }

    private Card getRandomCardFromDeck(User user) {
        List<Card> deck = user.getDeck();
        if (deck.isEmpty()) {
            // Handle case where user has no cards left
            return null;
        }
        return deck.get(random.nextInt(deck.size()));
    }
}
