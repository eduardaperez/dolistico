package juliokozarewicz.accounts.controllers;

import jakarta.servlet.http.HttpServletRequest;
import juliokozarewicz.accounts.services.AccountsLinkDeleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
class AccountsLinkDeleteController {

    // ==================================================== ( constructor init )

    private final AccountsLinkDeleteService accountsLinkDeleteService;

    public AccountsLinkDeleteController(

        AccountsLinkDeleteService accountsLinkDeleteService

    ) {

        this.accountsLinkDeleteService = accountsLinkDeleteService;

    }

    // ===================================================== ( constructor end )

    @PostMapping("/${ACCOUNTS_BASE_URL}/delete-account-link")
    @SuppressWarnings("unchecked")
    public ResponseEntity handle( HttpServletRequest request ) {

        // Auth endpoint
        Map<String, Object> credentialsData = (Map<String, Object>)
            request.getAttribute("credentialsData");

        return accountsLinkDeleteService.execute(
            credentialsData
        );

    }

}