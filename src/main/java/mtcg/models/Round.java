package mtcg.models;
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

    public void executeRound() {
        // Assuming playerOneCard and playerTwoCard are the cards in this round
        double playerOneDamage = playerOneCard.getDamage();
        double playerTwoDamage = playerTwoCard.getDamage();

        System.out.println(String.format("The fight is between %s - %.0f and %s - %.0f", playerOneCard.getName(), playerOneDamage, playerTwoCard.getName(), playerTwoDamage));

// Apply special rules first
        if (specialRuleApplies(playerOneCard, playerTwoCard)) {
            playerOneDamage = 0; // Adjust damage based on special rules
        }
        if (specialRuleApplies(playerTwoCard, playerOneCard)) {
            playerTwoDamage = 0; // Adjust damage based on special rules
        }

// Apply elemental effectiveness only if no special rules were applied
        if (playerOneDamage > 0 && playerTwoDamage > 0) {
            if (playerOneCard instanceof SpellCard || playerTwoCard instanceof SpellCard) {
                playerOneDamage = calculateElementalDamage(playerOneCard, playerTwoCard);
                playerTwoDamage = calculateElementalDamage(playerTwoCard, playerOneCard);
            }
        }


        System.out.println(String.format("Now the DAMAGES are %.0f VS %.0f", playerOneDamage, playerTwoDamage));
        // Determine the round winner
        if (playerOneDamage > playerTwoDamage) {
            // Player One wins the round
            System.out.println("Player 1 wins");
            System.out.println(String.format("%s beats %s", playerOneCard.getName(), playerTwoCard.getName()));
            transferCardToStack(playerTwo, playerOne, playerTwoCard); // Transfer card from Player Two's deck to Player One's stack
        } else if (playerTwoDamage > playerOneDamage) {
            // Player Two wins the round
            System.out.println("Player 2 wins");
            System.out.println(String.format("%s beats %s", playerTwoCard.getName(), playerOneCard.getName()));
            transferCardToStack(playerOne, playerTwo, playerOneCard); // Transfer card from Player One's deck to Player Two's stack
        } else {
            // It's a draw
            System.out.println("It's Draw");
        }

        // Other round logic (like transferring defeated card, updating stats, etc.)
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
        fromUser.getDeck().remove(card);
        toUser.getStack().add(card);
    }

}
