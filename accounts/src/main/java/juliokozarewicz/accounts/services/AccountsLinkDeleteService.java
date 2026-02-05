package juliokozarewicz.accounts.services;

import jakarta.transaction.Transactional;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.enums.EmailResponsesEnum;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountsLinkDeleteService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;

    @Value("${PUBLIC_DOMAIN}")
    private String publicDomain;

    @Value("${DELETE_ACCOUNT_LINK}")
    private String deleteAccountLink;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final AccountsRepository accountsRepository;
    private final AccountsManagementService accountsManagementService;
    private final EncryptionService encryptionService;

    public AccountsLinkDeleteService(

        MessageSource messageSource,
        AccountsRepository accountsRepository,
        AccountsManagementService accountsManagementService,
        EncryptionService encryptionService

    ) {

        this.messageSource = messageSource;
        this.accountsRepository = accountsRepository;
        this.accountsManagementService = accountsManagementService;
        this.encryptionService  = encryptionService;

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        Map<String, Object> credentialsData

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // Credentials
        String emailUser = credentialsData.get("email").toString();

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            emailUser
        );

        if (

            findUser.isPresent() &&
            !findUser.get().isBanned()

        ) {

            // Create token
            String tokenGenerated = accountsManagementService
                .createVerificationToken(
                    findUser.get().getId(),
                    findUser.get().getEmail(),
                    AccountsUpdateEnum.DELETE_ACCOUNT
                );

            // Link
            String linkFinal = UriComponentsBuilder
                .fromHttpUrl(deleteAccountLink)
                .queryParam("token", tokenGenerated)
                .build()
                .toUriString();

            // send email
            accountsManagementService.sendEmailStandard(
                emailUser,
                EmailResponsesEnum.ACCOUNT_DELETE_CLICK,
                linkFinal
            );

            // Revoke all tokens
            accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
                findUser.get().getId()
            );

        }
        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/delete-account-link");
        customLinks.put("next", "/" + accountsBaseURL + "/delete-account");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_delete_account_sent_success",
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