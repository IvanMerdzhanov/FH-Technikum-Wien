package mtcg.server.models;

import java.util.UUID;

public class TradeOfferRequest {
    private String receivingUsername;
    private String offeringUsername;
    private UUID cardId; // Can be null if only coins are offered
    private int coins; // Can be 0 if only a card is offered
    private Integer cardIndex;
    public String getOfferingUsername() {
        return offeringUsername;
    }

    public void setOfferingUsername(String offeringUsername) {
        this.offeringUsername = offeringUsername;
    }

    public void setCardIndex(Integer cardIndex) {
        this.cardIndex = cardIndex;
    }

    public Integer  getCardIndex() {
        return cardIndex;
    }

    public void setCardIndex(int cardIndex) {
        this.cardIndex = cardIndex;
    }

    public String getReceivingUsername() {
        return receivingUsername;
    }

    public void setReceivingUsername(String receivingUsername) {
        this.receivingUsername = receivingUsername;
    }

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

}
