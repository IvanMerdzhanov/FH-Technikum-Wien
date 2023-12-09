package mtcg.services;

import mtcg.models.Card;
import mtcg.models.ElementType;
import mtcg.models.MonsterCard;
import mtcg.models.SpellCard;
import mtcg.server.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PackageService {
    public static List<Card> getPackageCards() {
        List<Card> cards = new ArrayList<>();
        String query = "SELECT * FROM cards ORDER BY RANDOM() LIMIT 5"; // Get 5 random cards

        try (Connection conn = DatabaseConnector.connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Card card = createCardFromResultSet(rs);
                cards.add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions, maybe log them or throw a custom exception
        }
        return cards;
    }

    private static Card createCardFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("card_id");
        String name = rs.getString("name");
        double damage = rs.getDouble("damage");
        ElementType elementType = ElementType.valueOf(rs.getString("element_type").toUpperCase());
        String cardType = rs.getString("card_type");

        if ("monster".equalsIgnoreCase(cardType)) {
            return new MonsterCard(id, name, damage, elementType);
        } else if ("spell".equalsIgnoreCase(cardType)) {
            return new SpellCard(id, name, damage, elementType);
        } else {
            throw new SQLException("Unknown card type: " + cardType);
        }
    }


    // Add more methods as needed, like addCardsToUserStack, updateCardOwnership, etc.
}
