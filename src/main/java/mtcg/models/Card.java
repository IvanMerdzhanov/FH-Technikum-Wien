package mtcg.models;

public abstract class Card {
    private String id; // Unique identifier for the card. Could be a UUID or some other unique string.
    private String name;
    private double damage;
    private ElementType elementType;

    public Card(String id, String name, double damage, ElementType elementType) {
        this.id = id;
        this.name = name;
        this.damage = damage;
        this.elementType = elementType;
    }

    // Abstract methods that can be overridden by subclasses
    public abstract void specialEffect(Card opponentCard);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }
}
