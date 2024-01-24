package mtcg.models;

import java.util.ArrayList;
import java.util.List;

public class Round {
    private Card playerOneCard;
    private Card playerTwoCard;
    private User playerOne;
    private User playerTwo;

    public Round(User playerOne, Card playerOneCard, User playerTwo, Card playerTwoCard) {
        this.playerOne = playerOne;
        this.playerOneCard = playerOneCard;
        this.playerTwo = playerTwo;
        this.playerTwoCard = playerTwoCard;
    }

    public List<String> executeRound() {
        List<String> roundDetails = new ArrayList<>();
        double playerOneDamage = playerOneCard.getDamage();
        double playerTwoDamage = playerTwoCard.getDamage();

        roundDetails.add(String.format("Player One's card: %s (ID: %s) - Initial Damage: %.0f", playerOneCard.getName(), playerOneCard.getId(), playerOneDamage));
        roundDetails.add(String.format("Player Two's card: %s (ID: %s) - Initial Damage: %.0f", playerTwoCard.getName(), playerTwoCard.getId(), playerTwoDamage));

        // Apply special rules first
        if (specialRuleApplies(playerOneCard, playerTwoCard)) {
            roundDetails.add("Special Rule Applied: " + playerOneCard.getName() + " vs " + playerTwoCard.getName());
            playerOneDamage = 0;
        }
        if (specialRuleApplies(playerTwoCard, playerOneCard)) {
            roundDetails.add("Special Rule Applied: " + playerTwoCard.getName() + " vs " + playerOneCard.getName());
            playerTwoDamage = 0;
        }

        // Apply elemental effectiveness only if no special rules were applied
        if (playerOneDamage > 0 && playerTwoDamage > 0) {
            if (playerOneCard instanceof SpellCard || playerTwoCard instanceof SpellCard) {
                roundDetails.add("Elemental Effectiveness Applied");
                playerOneDamage = calculateElementalDamage(playerOneCard, playerTwoCard);
                playerTwoDamage = calculateElementalDamage(playerTwoCard, playerOneCard);
            }
        }

        roundDetails.add(String.format("Adjusted Damages: Player One - %.0f, Player Two - %.0f", playerOneDamage, playerTwoDamage));

        // Determine the round winner
        if (playerOneDamage > playerTwoDamage) {
            roundDetails.add(String.format("Player 1 wins: %s beats %s", playerOneCard.getName(), playerTwoCard.getName()));
            transferCardToStack(playerTwo, playerOne, playerTwoCard);
        } else if (playerTwoDamage > playerOneDamage) {
            roundDetails.add(String.format("Player 2 wins: %s beats %s", playerTwoCard.getName(), playerOneCard.getName()));
            transferCardToStack(playerOne, playerTwo, playerOneCard);
        } else {
            roundDetails.add("It's a draw between " + playerOneCard.getName() + " and " + playerTwoCard.getName());
        }

        return roundDetails;
    }


    private double calculateElementalDamage(Card attackingCard, Card defendingCard) {
        double damage = attackingCard.getDamage();
        ElementType attackingElement = attackingCard.getElementType();
        ElementType defendingElement = defendingCard.getElementType();

        // Check for elemental effectiveness
        if (attackingElement == ElementType.WATER && defendingElement == ElementType.FIRE) {
            damage *= 2; // Water is effective against Fire (double damage)
        } else if (attackingElement == ElementType.FIRE && defendingElement == ElementType.NORMAL) {
            damage *= 2; // Fire is effective against Normal (double damage)
        } else if (attackingElement == ElementType.NORMAL && defendingElement == ElementType.WATER) {
            damage *= 2; // Normal is effective against Water (double damage)
        } else if ((attackingElement == ElementType.FIRE && defendingElement == ElementType.WATER) ||
                (attackingElement == ElementType.WATER && defendingElement == ElementType.NORMAL) ||
                (attackingElement == ElementType.NORMAL && defendingElement == ElementType.FIRE)) {
            damage /= 2; // Attacking element is not effective (half damage)
        }
        // No change in damage for other combinations or if both cards are monsters

        return damage;
    }


    private boolean specialRuleApplies(Card attackingCard, Card defendingCard) {
        String attackingCardType = attackingCard.getName();
        String defendingCardType = defendingCard.getName();

        if (attackingCardType.equals("Goblin") && defendingCardType.equals("Dragon")) {
            return true; // Goblins are too afraid of Dragons to attack (Goblin's damage becomes 0)
        } else if (attackingCardType.equals("Ork") && defendingCardType.equals("Wizard")) {
            return true; // Orks are controlled by Wizards and cannot damage them (Ork's damage becomes 0)
        } else if (attackingCardType.equals("Knight") && defendingCard instanceof SpellCard && defendingCard.getElementType() == ElementType.WATER) {
            return true; // Knights are drowned by Water Spells (Knight's damage becomes 0)
        } else if (attackingCard instanceof SpellCard && defendingCardType.equals("Kraken")) {
            return true; // Spells have no effect on Kraken (Spell's damage becomes 0)
        } else if (attackingCardType.equals("Dragon") && defendingCardType.equals("FireElf")) {
            return true; // Dragons cannot damage Fire Elves (Dragon's damage becomes 0)
        }

        return false; // No special rule applies that affects attacking card's damage
    }

    private void transferCardToStack(User fromUser, User toUser, Card card) {
        fromUser.getStack().remove(card); // Remove card from loser's stack
        toUser.getStack().add(card); // Add card to winner's stack
    }


}
