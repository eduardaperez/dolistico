package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsLinkUpdateEmailDTO;
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
import org.springframework.web.util.UriComponentsBuilder;
import java.net.HttpURLConnection;

import java.net.HttpURLConnection;
import java.net.URI;
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
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final ErrorHandler errorHandler;
    private final AccountsManagementService accountsManagementService;
    private final EncryptionService encryptionService;
    private final AccountsRepository accountsRepository;

    public AccountsLinkUpdateEmailService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        AccountsManagementService accountsManagementService,
        EncryptionService encryptionService,
        AccountsRepository accountsRepository

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsManagementService = accountsManagementService;
        this.encryptionService = encryptionService;
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

        ////////////////////////////////////// ( Verify and authorize URL INIT )
        boolean isAllowedURL = false;

        try {

            String linkRaw = accountsLinkUpdateEmailDTO.link().trim();

            // Ensure scheme exists (default to HTTPS)
            if (!linkRaw.startsWith("http://") && !linkRaw.startsWith("https://")) {
                linkRaw = "https://" + linkRaw;
            }

            URI linkUri = new URI(linkRaw);

            // Enforce HTTPS only
            if (!"https".equalsIgnoreCase(linkUri.getScheme())) {
                isAllowedURL = false;
            } else if (linkUri.getFragment() != null) {
                // Reject fragments (#)
                isAllowedURL = false;
            } else {

                String linkHost = linkUri.getHost();

                if (linkHost == null) {
                    isAllowedURL = false;
                } else {

                    linkHost = linkHost.toLowerCase();

                    // Block common redirect-related query parameters
                    String query = linkUri.getQuery();
                    if (query != null) {
                        String q = query.toLowerCase();
                        if (
                            q.contains("redirect=") ||
                                q.contains("next=") ||
                                q.contains("url=") ||
                                q.contains("return=") ||
                                q.contains("continue=") ||
                                q.contains("target=")
                        ) {
                            isAllowedURL = false;
                        }
                    }

                    // Validate against allowed domains (exact match only)
                    String[] allowedOrigins = publicDomain.split(",");

                    for (String origin : allowedOrigins) {

                        String originTrimmed = origin.trim();

                        if (!originTrimmed.startsWith("http://") && !originTrimmed.startsWith("https://")) {
                            originTrimmed = "https://" + originTrimmed;
                        }

                        URI originUri = new URI(originTrimmed);
                        String originHost = originUri.getHost();

                        if (originHost != null &&
                            linkHost.equalsIgnoreCase(originHost)) {

                            isAllowedURL = true;
                            break;
                        }
                    }

                    // Optional: detect real HTTP redirects (3xx)
                    if (isAllowedURL) {
                        try {
                            HttpURLConnection connection =
                                (HttpURLConnection) linkUri.toURL().openConnection();

                            connection.setInstanceFollowRedirects(false);
                            connection.setRequestMethod("HEAD");
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);

                            int status = connection.getResponseCode();
                            if (status >= 300 && status < 400) {
                                isAllowedURL = false;
                            }
                        } catch (Exception ignored) {
                            // Any access error invalidates the URL
                            isAllowedURL = false;
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            // Any parsing or unexpected error invalidates the URL
            isAllowedURL = false;
        }

        if (!isAllowedURL) {

            // Single and consistent error response
            errorHandler.customErrorThrow(
                403,
                messageSource.getMessage(
                    "validation_valid_link", null, locale
                )
            );
        }
        /////////////////////////////////////// ( Verify and authorize URL END )

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

        // Create pin
        String pinGenerated = accountsManagementService.createVerificationPin(
            idUser,
            AccountsUpdateEnum.UPDATE_EMAIL,
            accountsLinkUpdateEmailDTO.newEmail()
        );

        // Send pin to new email
        accountsManagementService.sendEmailStandard(
            accountsLinkUpdateEmailDTO.newEmail().toLowerCase(),
            EmailResponsesEnum.UPDATE_EMAIL_PIN,
            pinGenerated
        );

        // Create token
        String tokenGenerated = accountsManagementService.createVerificationToken(
            idUser,
            AccountsUpdateEnum.UPDATE_EMAIL
        );

        // Link
        String linkFinal = UriComponentsBuilder
            .fromHttpUrl(accountsLinkUpdateEmailDTO.link())
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