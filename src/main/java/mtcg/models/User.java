package mtcg.models;

import java.util.ArrayList;
import java.util.List;
public class User {
    // Fields
    private String username;
    private String password;
    private int coins;
    private List<Card> stack;  // Collection of all the user's cards
    private List<Card> deck;   // Selected 4 cards for battling
    private UserStats userStats;
    //private String token; ОЩЕ НЕ ЗНАМ КАКВО Е ТОВА


    // Constructor
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20;  // Initial coins as per requirement
        this.stack = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.userStats = new UserStats();
      //  this.token = "";  // This can be set upon successful login/registration
    }

    public void addCardtoDeck(Card card){
        deck.add(card);
    }
    public void removeCardFromDeck(){}

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

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public UserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStats userStats) {
        this.userStats = userStats;
    }

    //public String getToken() {return token;}

    //public void setToken(String token) {this.token = token;}

}
