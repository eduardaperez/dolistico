package juliokozarewicz.accounts.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountsActivateDTO(

    @NotEmpty(message = "{validation_is_required}")
    @Pattern(
        regexp = "^[^<>&'\"/]*$",
        message = "{validation_disallowed_characters}"
    )
    String token

) {}