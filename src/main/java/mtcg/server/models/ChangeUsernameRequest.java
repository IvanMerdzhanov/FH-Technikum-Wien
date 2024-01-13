package mtcg.server.models;

public class ChangeUsernameRequest {
    private String newUsername;

    // Constructor
    public ChangeUsernameRequest() {}

    // Getters and setters
    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
}
