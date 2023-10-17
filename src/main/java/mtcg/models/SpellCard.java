package mtcg.models;

import mtcg.models.Card;
import mtcg.models.ElementType;

public class SpellCard extends Card {

    public SpellCard(String id, String name, double damage, ElementType elementType) {
        super(id, name, damage, elementType);
    }

    @Override
    public void specialEffect(Card opponentCard) {
        // Logic specific to SpellCard
    }
}