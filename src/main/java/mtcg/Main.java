package mtcg;

import mtcg.models.*;

public class Main {
    public static void main(String[] args) {
        // Create two users
        User user1 = new User("Player1", "pass1");
        User user2 = new User("Player2", "pass2");

        // TODO: Add cards to each user's deck
        // For example: user1.getDeck().add(new MonsterCard(...));
        //              user2.getDeck().add(new SpellCard(...));

        // Create a Battle
        Battle battle = new Battle(user1, user2);

        // Start the Battle
        battle.startBattle();

        // Print the result of the battle
        // This is a placeholder since we don't have the actual battle logic yet
        System.out.println("Battle completed.");
    }
}
