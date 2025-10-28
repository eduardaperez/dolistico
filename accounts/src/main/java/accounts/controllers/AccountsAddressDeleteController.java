package accounts.controllers;

import accounts.dtos.AccountsAddressDeleteDTO;
import accounts.services.AccountsAddressDeleteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
class AccountsAddressDeleteController {

    // ==================================================== ( constructor init )

    private final AccountsAddressDeleteService accountsAddressDeleteService;

    public AccountsAddressDeleteController(

        AccountsAddressDeleteService accountsAddressDeleteService

    ) {

        this.accountsAddressDeleteService = accountsAddressDeleteService;

    }

    // ===================================================== ( constructor end )

    @DeleteMapping("//${ACCOUNTS_BASE_URL}/delete-address")
    @SuppressWarnings("unchecked")
    public ResponseEntity handle(

        @Valid @RequestBody AccountsAddressDeleteDTO accountsAddressDeleteDTO,
        BindingResult bindingResult,
        HttpServletRequest request

    ) {

        // Auth endpoint
        Map<String, Object> credentialsData = (Map<String, Object>)
        request.getAttribute("credentialsData");

        return accountsAddressDeleteService.execute(
            credentialsData,
            accountsAddressDeleteDTO
        );

    }

}