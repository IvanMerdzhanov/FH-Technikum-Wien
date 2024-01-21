package mtcg.services;

import mtcg.models.Card;
import mtcg.models.ElementType;
import mtcg.models.MonsterCard;
import mtcg.models.SpellCard;
import mtcg.server.database.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PackageService {
    private final DatabaseConnector databaseConnector;

    // Constructor that accepts a DatabaseConnector
    public PackageService(DatabaseConnector databaseConnector) {
        this.databaseConnector = databaseConnector;
    }

    public List<Card> getPackageCards() {
        List<Card> cards = new ArrayList<>();
        String query = "SELECT * FROM cards WHERE taken = false ORDER BY RANDOM() LIMIT 5"; // Get 5 random cards
        String updateQuery = "UPDATE cards SET taken = true WHERE card_id = ?"; // Query to mark the card as taken

        try (Connection conn = databaseConnector.connect(); // Use the DatabaseConnector field
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Card card = createCardFromResultSet(rs);
                cards.add(card);

                // Mark the card as taken
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setObject(1, card.getId());
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions, maybe log them or throw a custom exception
        }
        return cards;
    }
    private static Card createCardFromResultSet(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("card_id"));
        double damage = rs.getDouble("damage");
        ElementType elementType = ElementType.valueOf(rs.getString("element_type").toUpperCase());
        String cardType = rs.getString("card_type");

        if ("monster".equalsIgnoreCase(cardType)) {
            String name = rs.getString("name");
            return new MonsterCard(id, name, damage, elementType);
        } else if ("spell".equalsIgnoreCase(cardType)) {
            // Set name for spell cards based on their element type
            String name = elementType.toString() + "Spell";
            return new SpellCard(id, name, damage, elementType);
        } else {
            throw new SQLException("Unknown card type: " + cardType);
        }
    }
    public void setCardAsNotTaken(UUID cardId) {
        String updateQuery = "UPDATE cards SET taken = false WHERE card_id = ?";
        try (Connection conn = databaseConnector.connect();
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

            updateStmt.setObject(1, cardId);
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
    }

    public Card getCardById(UUID cardId) {
        String query = "SELECT * FROM cards WHERE card_id = ?";
        try (Connection conn = databaseConnector.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setObject(1, cardId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return createCardFromResultSet(rs);
            } else {
                return null; // Card not found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
