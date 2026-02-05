package juliokozarewicz.accounts.controllers;

import juliokozarewicz.accounts.dtos.AccountsRequestDTO;
import juliokozarewicz.accounts.dtos.AccountsUpdateEmailDTO;
import juliokozarewicz.accounts.services.AccountsUpdateEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@Validated
class AccountsUpdateEmailController {

    // ==================================================== ( constructor init )

    private final AccountsUpdateEmailService accountsUpdateEmailService;
    private final AccountsRequestDTO accountsRequestDTO;

    public AccountsUpdateEmailController(

        AccountsUpdateEmailService accountsUpdateEmailService,
        AccountsRequestDTO accountsRequestDTO

    ) {

        this.accountsUpdateEmailService = accountsUpdateEmailService;
        this.accountsRequestDTO = accountsRequestDTO;

    }

    // ===================================================== ( constructor end )

    @PatchMapping("/${ACCOUNTS_BASE_URL}/update-email")
    @SuppressWarnings("unchecked")
    public ResponseEntity handle(

        // dtos errors
        @Valid @RequestBody AccountsUpdateEmailDTO accountsUpdateEmailDTO,
        BindingResult bindingResult,

        HttpServletRequest request

    ) {

        // Request data
        // ---------------------------------------------------------------------

        // user log
        String userIp = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(ip -> !ip.isBlank())
            .map(ip -> ip.contains(",") ? ip.split(",")[0].trim() : ip)
            .orElse(request.getRemoteAddr());

        String userAgent = request.getHeader("User-Agent");

        //validation request data
        accountsRequestDTO.validateUserIp(userIp);
        accountsRequestDTO.validateUserAgent(userAgent);
        // ---------------------------------------------------------------------

        return accountsUpdateEmailService.execute(
            userIp,
            userAgent,
            accountsUpdateEmailDTO
        );

    }

}