package juliokozarewicz.accounts.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import juliokozarewicz.accounts.dtos.AccountsLoginConfirmationDTO;
import juliokozarewicz.accounts.dtos.AccountsRequestDTO;
import juliokozarewicz.accounts.services.AccountsLoginConfirmationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Validated
class AccountsLoginConfirmationController {

    // ==================================================== ( constructor init )

    private final AccountsLoginConfirmationService accountsLoginConfirmationService;
    private final AccountsRequestDTO accountsRequestDTO;

    public AccountsLoginConfirmationController(

        AccountsLoginConfirmationService accountsLoginConfirmationService,
        AccountsRequestDTO accountsRequestDTO

    ) {

        this.accountsLoginConfirmationService = accountsLoginConfirmationService;
        this.accountsRequestDTO = accountsRequestDTO;

    }
    // ===================================================== ( constructor end )

    @PostMapping("/${ACCOUNTS_BASE_URL}/login-confirmation")
    public ResponseEntity handle(

        // dtos errors
        @Valid @RequestBody
        AccountsLoginConfirmationDTO accountsLoginConfirmationDTO,
        BindingResult bindingResult,

        HttpServletRequest request

    ) {

        // Request data
        // ---------------------------------------------------------------------
        String userIp = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(ip -> !ip.isBlank())
            .map(ip -> ip.contains(",") ? ip.split(",")[0].trim() : ip)
            .orElse(request.getRemoteAddr());

        String userAgent = request.getHeader("User-Agent");

        //validation request data
        accountsRequestDTO.validateUserIp(userIp);
        accountsRequestDTO.validateUserAgent(userAgent);
        // ---------------------------------------------------------------------

        return accountsLoginConfirmationService.execute(
            userIp,
            userAgent,
            accountsLoginConfirmationDTO
        );

    }

}