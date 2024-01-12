package mtcg.server.models;

public class TradeOfferRequest {
    private String offeringUsername;
    private String receivingUsername;
    private Integer offeredCardIndex; // Index of the card offered by the offering user
    private Integer requestedCardIndex; // Index of the card requested from the receiving user
    private int offeredCoins; // Coins offered by the offering user
    private int requestedCoins; // Coins requested from the receiving user

    // Getters and setters

    public String getOfferingUsername() {
        return offeringUsername;
    }

    public void setOfferingUsername(String offeringUsername) {
        this.offeringUsername = offeringUsername;
    }

    public String getReceivingUsername() {
        return receivingUsername;
    }

    public void setReceivingUsername(String receivingUsername) {
        this.receivingUsername = receivingUsername;
    }

    public Integer getOfferedCardIndex() {
        return offeredCardIndex;
    }

    public void setOfferedCardIndex(Integer offeredCardIndex) {
        this.offeredCardIndex = offeredCardIndex;
    }

    public Integer getRequestedCardIndex() {
        return requestedCardIndex;
    }

    public void setRequestedCardIndex(Integer requestedCardIndex) {
        this.requestedCardIndex = requestedCardIndex;
    }

    public int getOfferedCoins() {
        return offeredCoins;
    }

    public void setOfferedCoins(int offeredCoins) {
        this.offeredCoins = offeredCoins;
    }

    public int getRequestedCoins() {
        return requestedCoins;
    }

    public void setRequestedCoins(int requestedCoins) {
        this.requestedCoins = requestedCoins;
    }
}
