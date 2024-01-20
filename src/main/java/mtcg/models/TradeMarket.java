package mtcg.models;

import java.util.ArrayList;
import java.util.List;

public class TradeMarket {
    private static List<Card> cardsInTrade = new ArrayList<>();

    public static void addCardToMarket(Card card) {
        cardsInTrade.add(card);
    }

    public static void removeFromMarket(Card card) {
        cardsInTrade.remove(card);
    }

    public static boolean isCardInMarket(Card card) {
        return cardsInTrade.contains(card);
    }
}
