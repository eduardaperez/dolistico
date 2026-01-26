package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsLoginDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.enums.EmailResponsesEnum;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import jakarta.transaction.Transactional;
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
public class AccountsLoginService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final ErrorHandler errorHandler;
    private final EncryptionService encryptionService;
    private final AccountsRepository accountsRepository;
    private final AccountsManagementService accountsManagementService;

    public AccountsLoginService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        EncryptionService encryptionService,
        AccountsRepository accountsRepository,
        AccountsManagementService accountsManagementService

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsRepository = accountsRepository;
        this.encryptionService = encryptionService;
        this.accountsManagementService = accountsManagementService;

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        AccountsLoginDTO accountsLoginDTO

    ) {

        // Language
        Locale locale = LocaleContextHolder.getLocale();

        // Find user
        // ---------------------------------------------------------------------
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            accountsLoginDTO.email().toLowerCase()
        );
        // ---------------------------------------------------------------------

        // Invalid credentials
        // ---------------------------------------------------------------------
        if ( findUser.isEmpty() ) {

            // call custom error
            errorHandler.customErrorThrow(
                401,
                messageSource.getMessage(
                    "response_invalid_credentials", null, locale
                )
            );

        }
        // ---------------------------------------------------------------------

        // Password compare
        // ---------------------------------------------------------------------
        boolean passwordCompare = encryptionService.matchPasswords(
            accountsLoginDTO.password(),
            findUser.get().getPassword()
        );
        // ---------------------------------------------------------------------

        // Invalid credentials
        // ---------------------------------------------------------------------
        if ( !passwordCompare ) {

            // call custom error
            errorHandler.customErrorThrow(
                401,
                messageSource.getMessage(
                    "response_invalid_credentials", null, locale
                )
            );

        }
        // ---------------------------------------------------------------------

        // Account banned
        // ---------------------------------------------------------------------
        if ( findUser.get().isBanned() ) {

            // Revoke all tokens
            accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
                findUser.get().getId()
            );

            // send email
            accountsManagementService.sendEmailStandard(
                findUser.get().getEmail().toLowerCase(),
                EmailResponsesEnum.ACCOUNT_BANNED_ERROR,
                null
            );

            // call custom error
            errorHandler.customErrorThrow(
                403,
                messageSource.getMessage(
                    "response_login_error", null, locale
                )
            );

        }
        // ---------------------------------------------------------------------

        // Account deactivated
        // ---------------------------------------------------------------------
        if ( !findUser.get().isActive() ) {

            // Revoke all tokens
            accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
                findUser.get().getId()
            );

            // send email
            accountsManagementService.sendEmailStandard(
                findUser.get().getEmail().toLowerCase(),
                EmailResponsesEnum.ACCOUNT_EXIST_DEACTIVATED_ERROR,
                null
            );

            // call custom error
            errorHandler.customErrorThrow(
                403,
                messageSource.getMessage(
                    "response_login_error", null, locale
                )
            );

        }
        // ---------------------------------------------------------------------

        // Create user login token
        // ---------------------------------------------------------------------
        String userLoginToken=  accountsManagementService.createLoginToken(
            findUser.get().getId()
        );
        // ---------------------------------------------------------------------

        // Create user pin and sent
        // ---------------------------------------------------------------------
        // Create pin cache
        String pinGenerated = accountsManagementService.createVerificationPin(
            findUser.get().getId(),
            AccountsUpdateEnum.LOGIN_ACCOUNT,
            accountsLoginDTO.email().toLowerCase()
        );

        // Send pin to email
        accountsManagementService.sendEmailStandard(
            accountsLoginDTO.email().toLowerCase(),
            EmailResponsesEnum.LOGIN_PIN,
            pinGenerated
        );
        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/login");
        customLinks.put("next", "/" + accountsBaseURL + "/login-pin");

        // Tokens data
        Map<String, String> tokensData = new LinkedHashMap<>();
        tokensData.put("userLoginToken", userLoginToken);

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_login_credentials_success",
                    null,
                    locale
                )
            )
            .data(tokensData)
            .links(customLinks)
            .build();

        return ResponseEntity
            .status(response.getStatusCode())
            .body(response);
        // ---------------------------------------------------------------------

    }

}