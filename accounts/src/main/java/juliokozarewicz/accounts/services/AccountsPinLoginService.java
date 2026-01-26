package juliokozarewicz.accounts.services;

import jakarta.transaction.Transactional;
import juliokozarewicz.accounts.dtos.AccountsPinLoginDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.enums.EmailResponsesEnum;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountsPinLoginService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final ErrorHandler errorHandler;
    private final AccountsManagementService accountsManagementService;
    private final AccountsRepository accountsRepository;

    public AccountsPinLoginService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        AccountsManagementService accountsManagementService,
        AccountsRepository accountsRepository

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsManagementService = accountsManagementService;
        this.accountsRepository = accountsRepository;

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        AccountsPinLoginDTO accountsPinLoginDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            accountsPinLoginDTO.email()
        );

        if (findUser.isPresent()) {

            // Create pin cache
            String pinGenerated = accountsManagementService.createVerificationPin(
                findUser.get().getId(),
                AccountsUpdateEnum.LOGIN_ACCOUNT,
                accountsPinLoginDTO.email()
            );

            // Send pin to email
            accountsManagementService.sendEmailStandard(
                accountsPinLoginDTO.email().toLowerCase(),
                EmailResponsesEnum.LOGIN_PIN,
                pinGenerated
            );

        }

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/login-pin");
        customLinks.put("next", "/" + accountsBaseURL + "/login");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_pin_login_sent_success",
                    null,
                    locale
                )
            )
            .links(customLinks)
            .build();

        return ResponseEntity
            .status(response.getStatusCode())
            .body(response);

        // ---------------------------------------------------------------------

    }

}