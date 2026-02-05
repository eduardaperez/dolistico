package juliokozarewicz.accounts.services;

import jakarta.transaction.Transactional;
import juliokozarewicz.accounts.dtos.AccountsLinkUpdateEmailDTO;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class AccountsLinkUpdateEmailService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;

    @Value("${PUBLIC_DOMAIN}")
    private String publicDomain;

    @Value("${UPDATE_EMAIL_LINK}")
    private String updateEmailLink;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final ErrorHandler errorHandler;
    private final AccountsManagementService accountsManagementService;
    private final AccountsRepository accountsRepository;

    public AccountsLinkUpdateEmailService(

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

        Map<String, Object> credentialsData,
        AccountsLinkUpdateEmailDTO accountsLinkUpdateEmailDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // Credentials
        UUID idUser = UUID.fromString((String) credentialsData.get("id"));
        String emailUser = credentialsData.get("email").toString();

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            accountsLinkUpdateEmailDTO.newEmail()
        );

        if ( findUser.isPresent() ) {

            // call custom error
            errorHandler.customErrorThrow(
                409,
                messageSource.getMessage(
                    "response_update_email_sent_fail", null, locale
                )
            );

        }

        // process to change email
        // ---------------------------------------------------------------------

        // Create token
        String tokenGenerated = accountsManagementService.createVerificationToken(
            idUser,
            emailUser,
            AccountsUpdateEnum.UPDATE_EMAIL
        );

        // Create pin
        String pinGenerated = accountsManagementService.createVerificationPin(
            idUser,
            AccountsUpdateEnum.UPDATE_EMAIL,
            tokenGenerated,
            accountsLinkUpdateEmailDTO.newEmail()
        );

        // Send pin to new email
        accountsManagementService.sendEmailStandard(
            accountsLinkUpdateEmailDTO.newEmail().toLowerCase(),
            EmailResponsesEnum.UPDATE_EMAIL_PIN,
            pinGenerated
        );

        // Link
        String linkFinal = UriComponentsBuilder
            .fromHttpUrl(updateEmailLink)
            .queryParam("token", tokenGenerated)
            .build()
            .toUriString();

        // send link with token to old email
        accountsManagementService.sendEmailStandard(
            emailUser,
            EmailResponsesEnum.UPDATE_EMAIL_CLICK,
            linkFinal
        );

        // Revoke all tokens
        accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
            idUser
        );
        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/update-email-link");
        customLinks.put("next", "/" + accountsBaseURL + "/update-email");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_update_email_sent_success",
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