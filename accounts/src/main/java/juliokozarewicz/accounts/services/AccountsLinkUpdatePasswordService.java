package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsLinkUpdatePasswordDTO;
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
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountsLinkUpdatePasswordService {

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
    private final AccountsRepository accountsRepository;
    private final AccountsManagementService accountsManagementService;
    private final EncryptionService encryptionService;

    public AccountsLinkUpdatePasswordService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        AccountsRepository accountsRepository,
        AccountsManagementService accountsManagementService,
        EncryptionService encryptionService

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsRepository = accountsRepository;
        this.accountsManagementService = accountsManagementService;
        this.encryptionService  = encryptionService;

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        AccountsLinkUpdatePasswordDTO accountsLinkUpdatePasswordDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        ////////////////////////////////////// ( Verify and authorize URL INIT )
        boolean isAllowedURL = false;

        try {

            String linkRaw = accountsLinkUpdatePasswordDTO.link().trim();

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

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            accountsLinkUpdatePasswordDTO.email().toLowerCase()
        );

        if (

            findUser.isPresent() &&
            !findUser.get().isBanned()

        ) {

            // Delete all old tokens
            accountsManagementService.deleteAllVerificationTokenByIdUserNewTransaction(
                findUser.get().getId()
            );

            // Encrypted email
            String encryptedEmail = encryptionService.encrypt(
                accountsLinkUpdatePasswordDTO.email().toLowerCase()
            );

            // Create token
            String tokenGenerated = accountsManagementService
                .createVerificationToken(
                    findUser.get().getId(),
                    AccountsUpdateEnum.UPDATE_PASSWORD
                );

            // Link
            String linkFinal = UriComponentsBuilder
                .fromHttpUrl(accountsLinkUpdatePasswordDTO.link())
                .queryParam("email", encryptedEmail)
                .queryParam("token", tokenGenerated)
                .build()
                .toUriString();

            // send email
            accountsManagementService.sendEmailStandard(
                accountsLinkUpdatePasswordDTO.email().toLowerCase(),
                EmailResponsesEnum.UPDATE_PASSWORD_CLICK,
                linkFinal
            );

        }
        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/update-password-link");
        customLinks.put("next", "/" + accountsBaseURL + "/update-password");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_change_password_link_success",
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