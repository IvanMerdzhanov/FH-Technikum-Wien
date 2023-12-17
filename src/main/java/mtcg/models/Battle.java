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
        System.out.println("Battle started between " + playerOne.getUsername() + " and " + playerTwo.getUsername());
        for (int i = 0; i < 4; i++) {
            System.out.println("Round " + (i + 1) + " starts");
            Card cardOne = getRandomCardFromDeck(playerOne.getDeck());
            Card cardTwo = getRandomCardFromDeck(playerTwo.getDeck());

            if (cardOne == null || cardTwo == null) {
                System.out.println("Ending battle as one of the players ran out of cards.");
                break;
            }

            // Remove selected cards from each player's deck
            playerOne.getDeck().removeCard(cardOne);
            playerTwo.getDeck().removeCard(cardTwo);

            Round round = new Round(playerOne, cardOne, playerTwo, cardTwo);
            String roundResult = round.executeRound();
            System.out.println(roundResult);
            roundResults.add(roundResult);
        }

        String winner = determineWinner();
        System.out.println("Battle ended. Winner: " + winner);
        return new BattleOutcome(winner, roundResults);
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
            else {
                result.contains("Draw");
            }
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