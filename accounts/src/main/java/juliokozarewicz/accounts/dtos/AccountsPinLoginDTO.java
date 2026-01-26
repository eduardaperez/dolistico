package juliokozarewicz.accounts.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountsPinLoginDTO(

    @NotEmpty(message = "{validation_is_required}")
    @Size(max = 255, message = "{validation_many_characters}")
    @Email(message = "{validation_must_valid_email}")
    String email

) {}