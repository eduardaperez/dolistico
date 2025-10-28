package accounts.controllers;

import accounts.services.AccountsAddressGetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
class AccountsAddressGetController {

    // ==================================================== ( constructor init )

    private final AccountsAddressGetService accountsAddressGetService;

    public AccountsAddressGetController(

        AccountsAddressGetService accountsAddressGetService

    ) {

        this.accountsAddressGetService = accountsAddressGetService;

    }

    // ===================================================== ( constructor end )

    @GetMapping("/${ACCOUNTS_BASE_URL}/get-address")
    @SuppressWarnings("unchecked")
    public ResponseEntity handle(

        HttpServletRequest request

    ) {

        // Auth endpoint
        Map<String, Object> credentialsData = (Map<String, Object>)
        request.getAttribute("credentialsData");

        return accountsAddressGetService.execute(credentialsData);

    }

}