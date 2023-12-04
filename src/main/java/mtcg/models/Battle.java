package mtcg.models;

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

    public BattleOutcome startBattle() {
        for (int i = 0; i < 10; i++) { // Assuming a battle consists of 10 rounds
            Card cardOne = getRandomCardFromDeck(playerOne.getDeck());
            Card cardTwo = getRandomCardFromDeck(playerTwo.getDeck());

            if (cardOne == null || cardTwo == null) {
                break; // End the battle if either player runs out of cards
            }

            Round round = new Round(playerOne, cardOne, playerTwo, cardTwo);
            String roundResult = round.executeRound(); // Modify executeRound to return a result string
            roundResults.add(roundResult);
        }

        // Determine the overall winner and return the battle outcome
        String winner = determineWinner(); // Implement this method based on your game rules
        return new BattleOutcome(winner, roundResults); // BattleOutcome is a new class to hold battle results
    }

    private Card getRandomCardFromDeck(Deck deck) {
        if (deck.getCards().isEmpty()) {
            return null; // Handle case where the deck has no cards
        }
        return deck.getCards().get(random.nextInt(deck.getCards().size()));
    }

    public String determineWinner() {
        // Example logic: count the number of rounds won by each player
        int winsPlayerOne = 0;
        int winsPlayerTwo = 0;

        for (String result : roundResults) {
            if (result.contains("Player 1 wins")) {
                winsPlayerOne++;
            } else if (result.contains("Player 2 wins")) {
                winsPlayerTwo++;
            }
            // Include logic for draws if needed
        }

        // Determine the winner based on the number of wins
        if (winsPlayerOne > winsPlayerTwo) {
            return playerOne.getUsername();
        } else if (winsPlayerTwo > winsPlayerOne) {
            return playerTwo.getUsername();
        } else {
            return "Draw"; // or any other way you want to handle a draw
        }
    }
    public List<String> getRoundResults() {
        return roundResults;
    }
}