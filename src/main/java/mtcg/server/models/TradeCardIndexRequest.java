package mtcg.server.models;

public class TradeCardIndexRequest {
    private int tradeOfferIndex; // The index of the trade offer in the user's offer list
    private int cardChoiceIndex; // The index of the chosen card for the trade

    // Constructor
    public TradeCardIndexRequest() {
        // Default constructor
    }

    // Parameterized constructor
    public TradeCardIndexRequest(int tradeOfferIndex, int cardChoiceIndex) {
        this.tradeOfferIndex = tradeOfferIndex;
        this.cardChoiceIndex = cardChoiceIndex;
    }

    // Getters and setters
    public int getTradeOfferIndex() {
        return tradeOfferIndex;
    }

    public void setTradeOfferIndex(int tradeOfferIndex) {
        this.tradeOfferIndex = tradeOfferIndex;
    }

    public int getCardChoiceIndex() {
        return cardChoiceIndex;
    }

    public void setCardChoiceIndex(int cardChoiceIndex) {
        this.cardChoiceIndex = cardChoiceIndex;
    }
}
