package mtcg.models;

import java.util.ArrayList;
import java.util.List;

public class Package {
    private List<Card> cards;

    public Package() {
        this.cards = new ArrayList<>();
        // Initialize with 5 cards (either predetermined or randomly generated)
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards); // Return a copy of the list to prevent external modifications
    }

}
