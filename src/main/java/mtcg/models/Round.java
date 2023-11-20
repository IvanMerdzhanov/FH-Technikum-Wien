package mtcg.models;

public class Round {
    private Card playerOneCard;
    private Card playerTwoCard;
    private RoundResult result;

    public Round(Card playerOneCard, Card playerTwoCard) {
        this.playerOneCard = playerOneCard;
        this.playerTwoCard = playerTwoCard;
        this.result = determineRoundOutcome();
    }

    public void fight(Card playerOneCard, Card playerTwoCard) {
        // Store the cards for this round
        this.playerOneCard = playerOneCard;
        this.playerTwoCard = playerTwoCard;

        // Determine the outcome of the round
        this.result = determineRoundOutcome();
    }

    private RoundResult determineRoundOutcome() {
        double playerOneDamage = playerOneCard.getDamage();
        double playerTwoDamage = playerTwoCard.getDamage();

        if (playerOneDamage > playerTwoDamage) {
            // Player One's card wins the round
            return new RoundResult(playerOneCard, "Player One wins the round with " + playerOneCard.getName());
        } else if (playerTwoDamage > playerOneDamage) {
            // Player Two's card wins the round
            return new RoundResult(playerTwoCard, "Player Two wins the round with " + playerTwoCard.getName());
        } else {
            // It's a draw
            return new RoundResult(null, "The round is a draw!");
        }
    }

    private double calculateDamage(Card card) {
        // Calculate the damage of the card.
        // This method might include complex logic involving card attributes and special abilities.
        return card.getDamage(); // Placeholder for actual damage calculation
    }


    // Getters and setters for playerOneCard, playerTwoCard, and result

    public Card getPlayerOneCard() {
        return playerOneCard;
    }

    public void setPlayerOneCard(Card playerOneCard) {
        this.playerOneCard = playerOneCard;
    }

    public Card getPlayerTwoCard() {
        return playerTwoCard;
    }

    public void setPlayerTwoCard(Card playerTwoCard) {
        this.playerTwoCard = playerTwoCard;
    }

    public RoundResult getResult() {
        return result;
    }

    public void setResult(RoundResult result) {
        this.result = result;
    }
}
