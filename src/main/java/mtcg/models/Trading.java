package mtcg.models;

import java.util.UUID;

public class Trading {
    private UUID id;
    private User offeringUser;
    private User receivingUser;
    private Card offeredCard;
    private int offeredCoins;
    private Card requestedCard;
    private int requestedCoins;

    // Constructor for reciprocal trading
    public Trading(User offeringUser, User receivingUser, Card offeredCard, int offeredCoins, Card requestedCard, int requestedCoins) {
        this.id = UUID.randomUUID();
        this.offeringUser = offeringUser;
        this.receivingUser = receivingUser;
        this.offeredCard = offeredCard;
        this.offeredCoins = offeredCoins;
        this.requestedCard = requestedCard;
        this.requestedCoins = requestedCoins;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getOfferingUser() {
        return offeringUser;
    }

    public void setOfferingUser(User offeringUser) {
        this.offeringUser = offeringUser;
    }

    public User getReceivingUser() {
        return receivingUser;
    }

    public void setReceivingUser(User receivingUser) {
        this.receivingUser = receivingUser;
    }

    public Card getOfferedCard() {
        return offeredCard;
    }

    public void setOfferedCard(Card offeredCard) {
        this.offeredCard = offeredCard;
    }

    public int getOfferedCoins() {
        return offeredCoins;
    }

    public void setOfferedCoins(int offeredCoins) {
        this.offeredCoins = offeredCoins;
    }

    public Card getRequestedCard() {
        return requestedCard;
    }

    public void setRequestedCard(Card requestedCard) {
        this.requestedCard = requestedCard;
    }

    public int getRequestedCoins() {
        return requestedCoins;
    }

    public void setRequestedCoins(int requestedCoins) {
        this.requestedCoins = requestedCoins;
    }
    public String getOfferDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Offer from: ").append(offeringUser.getUsername());
        details.append(" to: ").append(receivingUser.getUsername());

        if (offeredCard != null || offeredCoins > 0) {
            details.append(", Offering: ");
            if (offeredCard != null) {
                details.append("Card: ").append(offeredCard.getName()).append(" (ID: ").append(offeredCard.getId());
            }
            if (offeredCoins > 0) {
                if (offeredCard != null) details.append(", ");
                details.append("Coins: ").append(offeredCoins);
            }
        }

        if (requestedCard != null || requestedCoins > 0) {
            details.append(", Requesting: ");
            if (requestedCard != null) {
                details.append("Card: ").append(requestedCard.getName()).append(" (ID: ").append(requestedCard.getId());
            }
            if (requestedCoins > 0) {
                if (requestedCard != null) details.append(", ");
                details.append("Coins: ").append(requestedCoins);
            }
        }

        return details.toString();
    }

}
