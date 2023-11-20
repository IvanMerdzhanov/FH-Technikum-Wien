package mtcg.models;

public class RoundResult {
    private Card winningCard;
    private String description;

    public RoundResult(Card winningCard, String description) {
        this.winningCard = winningCard;
        this.description = description;
    }

    // If you need to determine the User who won, you could have a method like:
    public User getWinningUser() {
        // You would need a way to get the User from the Card, or pass the User directly to the RoundResult.
        // For example, if the Card class has a reference to its User, you could return it here.
        // return winningCard.getOwner();
        return null; // Placeholder for actual implementation
    }

    // Getters for winningCard and description
    public Card getWinningCard() {
        return winningCard;
    }

    public String getDescription() {
        return description;
    }

    // Setters for winner and description, if needed

    public void setWinningCard(Card winningCard) {
        this.winningCard = winningCard;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
