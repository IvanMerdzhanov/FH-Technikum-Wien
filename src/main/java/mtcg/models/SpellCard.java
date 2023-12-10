package mtcg.models;

import java.util.UUID;

public class SpellCard extends Card {

    public SpellCard(UUID id, String name, double damage, ElementType elementType) {
        super(id, name, damage, elementType);
    }

    @Override
    public void specialEffect(Card opponentCard) {
        // Logic specific to SpellCard
    }
}