package mtcg.server.models;

public class ChangePasswordRequest {
    private String newPassword;

    public ChangePasswordRequest() {
        // Default constructor
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
