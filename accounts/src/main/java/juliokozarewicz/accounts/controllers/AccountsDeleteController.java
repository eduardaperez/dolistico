package juliokozarewicz.accounts.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import juliokozarewicz.accounts.dtos.AccountsDeleteDTO;
import juliokozarewicz.accounts.dtos.AccountsRequestDTO;
import juliokozarewicz.accounts.services.AccountsDeleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Validated
class AccountsDeleteController {

    // ==================================================== ( constructor init )

    private final AccountsDeleteService accountsDeleteService;
    private final AccountsRequestDTO accountsRequestDTO;

    public AccountsDeleteController(

        AccountsDeleteService accountsDeleteService,
        AccountsRequestDTO accountsRequestDTO

    ) {

        this.accountsDeleteService = accountsDeleteService;
        this.accountsRequestDTO = accountsRequestDTO;

    }

    // ===================================================== ( constructor end )

    @PostMapping("/${ACCOUNTS_BASE_URL}/delete")
    @SuppressWarnings("unchecked")
    public ResponseEntity handle(

        // dtos errors
        @Valid @RequestBody()
        AccountsDeleteDTO accountsDeleteDTO,

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

        return accountsDeleteService.execute(
            userIp,
            userAgent,
            accountsDeleteDTO
        );

    }

}