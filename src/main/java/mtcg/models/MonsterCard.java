package mtcg.models;

import java.util.UUID;

public class MonsterCard extends Card {

    public MonsterCard(UUID id, String name, double damage, ElementType elementType) {
        super(id, name, damage, elementType);
    }

    @Override
    public void specialEffect(Card opponentCard) {
        // Logic specific to MonsterCard
    }
}