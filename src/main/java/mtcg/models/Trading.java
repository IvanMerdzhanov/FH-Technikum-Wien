package mtcg.models;

import java.util.UUID;

public class Trading {
    private UUID id;
    private User offeringUser;
    private User receivingUser;
    private Card offeredCard;
    private int offeredCoins;
    private String requestedType; // "Spell", "Monster", or specific type
    private int minimumDamage;
    private String typeOfOffer;
    private String state;

    // Constructor for different types of trading
    public Trading(User offeringUser, User receivingUser, Card offeredCard, int offeredCoins, String requestedType, int minimumDamage, String typeOfOffer) {
        this.id = UUID.randomUUID();
        this.offeringUser = offeringUser;
        this.receivingUser = receivingUser;
        this.offeredCard = offeredCard;
        this.offeredCoins = offeredCoins;
        this.requestedType = requestedType;
        this.minimumDamage = minimumDamage;
        this.typeOfOffer = typeOfOffer;
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

    public String getRequestedType() {
        return requestedType;
    }

    public void setRequestedType(String requiredType) {
        this.requestedType = requiredType;
    }

    public int getMinimumDamage() {
        return minimumDamage;
    }

    public void setMinimumDamage(int minimumDamage) {
        this.minimumDamage = minimumDamage;
    }
    public String getTypeOfOffer() {
        return typeOfOffer;
    }

    public void setTypeOfOffer(String typeOfOffer) {
        this.typeOfOffer = typeOfOffer;
    }

    public User getReceivingUser() {
        return receivingUser;
    }

    public void setReceivingUser(User receivingUser) {
        this.receivingUser = receivingUser;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTradeDetails() {
        StringBuilder details = new StringBuilder("Trade Offer ID: ").append(id.toString());
        details.append("\nOffered by: ").append(offeringUser.getUsername());

        switch (typeOfOffer) {
            case "card-for-card":
                details.append("\nOffering Card: ").append(offeredCard != null ? offeredCard.getName() : "None");
                details.append("\nRequesting: ").append(requestedType).append(" with min damage: ").append(minimumDamage);
                break;
            case "card-for-coins":
                details.append("\nOffering Card: ").append(offeredCard != null ? offeredCard.getName() : "None");
                details.append("\nRequesting Coins: ").append(offeredCoins);
                break;
            case "coins-for-card":
                details.append("\nOffering Coins: ").append(offeredCoins);
                details.append("\nRequesting: ").append(requestedType).append(" with min damage: ").append(minimumDamage);
                break;
        }

        return details.toString();
    }
}
