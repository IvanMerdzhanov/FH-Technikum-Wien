package mtcg.server.models;

public class EditProfileRequest {
    private String newUsername;
    private String newPassword;

    // Constructor without parameters
    public EditProfileRequest() {
    }

    // Getters and setters
    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
