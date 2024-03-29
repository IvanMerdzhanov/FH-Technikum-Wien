package mtcg.models;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void removeCard(Card card) {
        cards.remove(card);
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public void clear() {
        cards.clear();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
