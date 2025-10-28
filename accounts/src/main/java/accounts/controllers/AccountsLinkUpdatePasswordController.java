package accounts.controllers;

import accounts.dtos.AccountsLinkUpdatePasswordDTO;
import accounts.services.AccountsLinkUpdatePasswordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
class AccountsLinkUpdatePasswordController {

    // ==================================================== ( constructor init )

    private final AccountsLinkUpdatePasswordService accountsLinkUpdatePasswordService;

    public AccountsLinkUpdatePasswordController(
        AccountsLinkUpdatePasswordService accountsLinkUpdatePasswordService
    ) {
        this.accountsLinkUpdatePasswordService =
            accountsLinkUpdatePasswordService;
    }
    // ===================================================== ( constructor end )

    @PostMapping("/${ACCOUNTS_BASE_URL}/update-password-link")
    public ResponseEntity handle(

        // dtos errors
        @Valid @RequestBody
        AccountsLinkUpdatePasswordDTO accountsLinkUpdatePasswordDTO,

        BindingResult bindingResult

    ) {

        return accountsLinkUpdatePasswordService.execute(
            accountsLinkUpdatePasswordDTO
        );

    }

}