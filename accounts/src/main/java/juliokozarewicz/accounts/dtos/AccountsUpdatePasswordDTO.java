package juliokozarewicz.accounts.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountsUpdatePasswordDTO(

    @NotEmpty(message = "{validation_is_required}")
    @Size(
        min = 8, max = 256,
        message = "{validation_must_eight_characters_long}"
    )
    @Pattern(
        regexp = "^[^<>&'\"/]*$",
        message = "{validation_disallowed_characters}"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
        message = "{validation_must_uppercase_letter}"
    )
    String password,

    @NotEmpty(message = "{validation_is_required}")
    @Pattern(
        regexp = "^[^<>&'\"/]*$",
        message = "{validation_disallowed_characters}"
    )
    String token

) {}
