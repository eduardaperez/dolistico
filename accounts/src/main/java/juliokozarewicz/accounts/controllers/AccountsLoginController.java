package juliokozarewicz.accounts.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import juliokozarewicz.accounts.dtos.AccountsLoginDTO;
import juliokozarewicz.accounts.dtos.AccountsRequestDTO;
import juliokozarewicz.accounts.services.AccountsLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
class AccountsLoginController {

    // ==================================================== ( constructor init )

    private final AccountsLoginService accountsLoginService;

    public AccountsLoginController(

        AccountsLoginService accountsLoginService

    ) {

        this.accountsLoginService = accountsLoginService;

    }

    // ===================================================== ( constructor end )

    @PostMapping("/${ACCOUNTS_BASE_URL}/login")
    public ResponseEntity handle(

        // dtos errors
        @Valid @RequestBody AccountsLoginDTO accountsLoginDTO,
        BindingResult bindingResult,

        HttpServletRequest request

    ) {

        return accountsLoginService.execute(
            accountsLoginDTO
        );

    }

}