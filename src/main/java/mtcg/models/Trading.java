package mtcg.models;

import java.util.UUID;

public class Trading {
    private UUID id;
    private User offeringUser;
    private User receivingUser;
    private Card offeredCard;
    private int offeredCoins;
    private TradeStatus status;


    // Constructor for trading a card
    public Trading(User offeringUser, User receivingUser, Card offeredCard) {
        this(offeringUser, receivingUser, offeredCard, 0);
    }

    // Constructor for trading coins
    public Trading(User offeringUser, User receivingUser, int offeredCoins) {
        this(offeringUser, receivingUser, null, offeredCoins);
    }

    // Constructor for trading a card and coins
    public Trading(User offeringUser, User receivingUser, Card offeredCard, int offeredCoins) {
        this.id = UUID.randomUUID();
        this.offeringUser = offeringUser;
        this.receivingUser = receivingUser;
        this.offeredCard = offeredCard;
        this.offeredCoins = offeredCoins;
        this.status = TradeStatus.PENDING;
    }
    public String getOfferDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Offer from: ").append(offeringUser.getUsername());
        details.append(" to: ").append(receivingUser.getUsername());
        if (offeredCard != null) {
            details.append(", Card: ").append(offeredCard.getName()).append(" (ID: ").append(offeredCard.getId()).append(")");
        }
        if (offeredCoins > 0) {
            if (offeredCard != null) {
                details.append(" and ");
            }
            details.append("Coins: ").append(offeredCoins);
        }
        details.append(", Status: ").append(status);
        return details.toString();
    }
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

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public enum TradeStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}
