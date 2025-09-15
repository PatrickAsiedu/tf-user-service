package app.tradeflows.api.user_service.dtos;

import lombok.Data;

@Data
public class ChangePasswordDto {
    private String currentPassword;
    private String changePassword;
    private String confirmChangePassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getChangePassword() {
        return changePassword;
    }

    public String getConfirmChangePassword() {
        return confirmChangePassword;
    }
}
