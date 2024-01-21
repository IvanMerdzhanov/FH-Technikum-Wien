package mtcg.models;

import java.util.ArrayList;
import java.util.List;
public class User {
    // Fields
    private String username;
    private String password;
    private int coins;
    private List<Card> stack;  // Collection of all the user's cards
    private Deck deck;   // Selected 4 cards for battling
    private String token;
    List<Trading> offers;
    private boolean canChangeCredentials;
    private UserStats userStats;


    // Constructor
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20;  // Initial coins as per requirement
        this.stack = new ArrayList<>();
        this.deck = new Deck();
        this.userStats = new UserStats(0, 0, 0, 69);
        this.token = "";
    }

    public List<Trading> getOffers() {
        return offers;
    }

    public void setOffers(List<Trading> offers) {
        this.offers = offers;
    }

    public User() {
        this.stack = new ArrayList<>();
        this.deck = new Deck();
    }


    public void addCardtoDeck(Card card) {
        this.deck.addCard(card);
    }
    public void removeCardFromDeck(){}

    public void spendCoins(){
        this.coins -= 5;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public List<Card> getStack() {
        return stack;
    }

    public void setStack(List<Card> stack) {
        this.stack = stack;
    }


    public Deck getDeck() {
        return deck;
    }


    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public boolean canChangeCredentials() {
        return canChangeCredentials;
    }
    public void setCanChangeCredentials(boolean canChangeCredentials) {
        this.canChangeCredentials = canChangeCredentials;
    }
}
