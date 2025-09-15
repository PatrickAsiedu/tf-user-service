package app.tradeflows.api.user_service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDto {
    @NotNull(message = "Full name is required")
    private String name;
    @NotNull(message = "Email Address is required")
    @Email
    private String email;
    private String password;
    private String dob;

    public @NotNull(message = "Full name is required") String getName() {
        return name;
    }

    public @NotNull(message = "Email Address is required") @Email String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getDob() {
        return dob;
    }
}
