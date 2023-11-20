package mtcg.models;

import java.util.ArrayList;
import java.util.List;

public class Battle {
    private List<Round> rounds;
    private User playerOne;
    private User playerTwo;
    private int currentRoundIndex;

    public Battle(User playerOne, User playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.rounds = new ArrayList<>();
        this.currentRoundIndex = 0;
    }

    public void startBattle() {
        System.out.println("Battle started between " + playerOne.getUsername() + " and " + playerTwo.getUsername());

        int numberOfRounds = Math.min(playerOne.getDeck().size(), playerTwo.getDeck().size());
        for (int i = 0; i < numberOfRounds; i++) {
            System.out.println("Preparing round " + (i + 1));
            Card playerOneCard = playerOne.getDeck().get(i);
            Card playerTwoCard = playerTwo.getDeck().get(i);
            Round round = new Round(playerOneCard, playerTwoCard);  // Correctly instantiating the Round
            rounds.add(round);

            System.out.println("Round " + (i + 1) + " starting.");
            performRound(playerOneCard, playerTwoCard);  // This method will handle the round logic
            System.out.println("Round " + (i + 1) + " completed.");
        }

        User winner = determineWinner();  // Implement this method to determine the overall winner
        System.out.println("Battle winner: " + (winner != null ? winner.getUsername() : "None"));
    }


    private void performRound(Card playerOneCard, Card playerTwoCard) {
        System.out.println("Player 1 card: " + playerOneCard.getName() + " vs Player 2 card: " + playerTwoCard.getName());
        // Calculate the damage dealt by each card
        double damagePlayerOne = calculateDamage(playerOneCard);
        double damagePlayerTwo = calculateDamage(playerTwoCard);

        // Determine the winner of the round
        if (damagePlayerOne > damagePlayerTwo) {
            System.out.println(playerOneCard.getName() + " wins the round!");
            // Update stats, round results, etc.
        } else if (damagePlayerTwo > damagePlayerOne) {
            System.out.println(playerTwoCard.getName() + " wins the round!");
            // Update stats, round results, etc.
        } else {
            System.out.println("The round ends in a draw.");
            // Handle a draw situation
        }
    }

    private double calculateDamage(Card card) {
        // For now, we return the card's damage.
        // Later, you can add more complex logic here, such as elemental weaknesses/strengths.
        return card.getDamage();
    }

    private User determineWinner() {
        // Determine the winner based on the results of the rounds
        // This could be based on remaining health, number of cards defeated, etc.
        // Return the winning User object
        return null; // Placeholder until you implement the logic
    }

    public List<Round> getRounds() {
        return rounds;
    }

    public void setRounds(List<Round> rounds) {
        this.rounds = rounds;
    }

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

    public int getCurrentRoundIndex() {
        return currentRoundIndex;
    }

    public void setCurrentRoundIndex(int currentRoundIndex) {
        this.currentRoundIndex = currentRoundIndex;
    }

    // Implement other necessary methods and logic...
}
