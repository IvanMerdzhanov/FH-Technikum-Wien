package mtcg;

import mtcg.models.*;

public class Main {
    public static void main(String[] args) {

        //It's for for the simulation
        User user1 = new User("Player1", "pass1");
        User user2 = new User("Player2", "pass2");

        Card[] cards = new Card[10];

        cards[0] = new MonsterCard("M1", "Fire Dragon", 50, ElementType.FIRE);
        cards[1] = new MonsterCard("M2", "Water Serpent", 45, ElementType.WATER);
        cards[2] = new MonsterCard("M3", "Earth Golem", 40, ElementType.NORMAL);
        cards[3] = new MonsterCard("M4", "Sky Griffin", 55, ElementType.NORMAL);
        cards[4] = new MonsterCard("M5", "Forest Elf", 35, ElementType.NORMAL);
        cards[5] = new SpellCard("S1", "Blazing Meteor", 60, ElementType.FIRE);
        cards[6] = new SpellCard("S2", "Tidal Wave", 50, ElementType.WATER);
        cards[7] = new SpellCard("S3", "Thunder Strike", 45, ElementType.NORMAL);
        cards[8] = new SpellCard("S4", "Wind Gust", 40, ElementType.NORMAL);
        cards[9] = new SpellCard("S5", "Earthquake", 55, ElementType.NORMAL);

        //It's for adding card to the deck for the simulation. Later do be changed
        for (int i = 0; i < 5; i++) {
            user1.addCardtoDeck(cards[i]);
            user2.addCardtoDeck(cards[i + 5]);
        }

        User user3 = new User("Player1", "pass1");
        User user4 = new User("Player2", "pass2");
        user3.addCardtoDeck(new MonsterCard("M5", "Knight", 15, ElementType.NORMAL));
        user4.addCardtoDeck(new SpellCard("S1", "WaterSpell", 10, ElementType.WATER));




        Battle battle = new Battle(user3, user4);
        battle.startBattle();
    }
}
