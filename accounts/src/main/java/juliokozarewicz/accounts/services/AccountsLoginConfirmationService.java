package juliokozarewicz.accounts.services;

import jakarta.transaction.Transactional;
import juliokozarewicz.accounts.dtos.AccountsCacheLoginConfirmationMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsCacheVerificationPinMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsCacheVerificationTokenMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsLoginConfirmationDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.enums.EmailResponsesEnum;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountsLoginConfirmationService {

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
    private final Cache loginTokenCache;
    private final Cache pinVerificationCache;

    public AccountsLoginConfirmationService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        AccountsManagementService accountsManagementService,
        AccountsRepository accountsRepository,
        CacheManager cacheManager

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsManagementService = accountsManagementService;
        this.accountsRepository = accountsRepository;
        this.loginTokenCache = cacheManager.getCache("accounts-loginCache");
        this.pinVerificationCache = cacheManager.getCache("accounts-pinVerificationCache");

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        String userIp,
        String userAgent,
        AccountsLoginConfirmationDTO accountsLoginConfirmationDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // find login token
        AccountsCacheLoginConfirmationMetaDTO findLoginToken = Optional.ofNullable(
                loginTokenCache.get(accountsLoginConfirmationDTO.userLoginToken())
        )
        .map(Cache.ValueWrapper::get)
        .map(AccountsCacheLoginConfirmationMetaDTO.class::cast)
        .orElse(null);

        // Invalid or not found login token
        if (findLoginToken == null) {
            errorHandler.customErrorThrow(
                401,
                messageSource.getMessage(
                    "response_invalid_credentials", null, locale
                )
            );
        }

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findById(
            UUID.fromString(findLoginToken.getIdUser())
        );

        // Invalid or not found user
        if (findUser.isEmpty()) {
            errorHandler.customErrorThrow(
                401,
                messageSource.getMessage(
                    "response_invalid_credentials", null, locale
                )
            );
        }

        // find login pin
        AccountsCacheVerificationPinMetaDTO findLoginPin = Optional.ofNullable(
            pinVerificationCache.get(
                findUser.get().getId() + "::" + accountsLoginConfirmationDTO.pin()
            )
        )
        .map(Cache.ValueWrapper::get)
        .map(AccountsCacheVerificationPinMetaDTO.class::cast)
        .filter(
            tokenMeta -> tokenMeta
                .getReason().equals(AccountsUpdateEnum.LOGIN_ACCOUNT)
        )
        .orElse(null);

        // Invalid or not found login PIN
        if (findLoginPin == null) {
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_login_pin_error", null, locale
                )
            );
        }

        // Cross token not allowed
        if (

            !findLoginPin.getLinked().equals(
                accountsLoginConfirmationDTO.userLoginToken()
            )

        ) {

            errorHandler.customErrorThrow(
                401,
                messageSource.getMessage(
                    "response_invalid_credentials", null, locale
                )
            );

        }

        // Create JWT
        String AccessCredential = accountsManagementService.createCredentialJWT(
            findUser.get().getEmail().toLowerCase()
        );

        // Create refresh token
        // ---------------------------------------------------------------------

        // Clean old tokens
        accountsManagementService.deleteExpiredRefreshTokensListById(
            findUser.get().getId()
        );

        String RefreshToken=  accountsManagementService.createRefreshLogin(
            findUser.get().getId(),
            userIp,
            userAgent,
            null
        );
        // ---------------------------------------------------------------------

        // Tokens data
        Map<String, String> tokensData = new LinkedHashMap<>();
        tokensData.put("access", AccessCredential);
        tokensData.put("refresh", RefreshToken);

        // Revoke login token and pin
        loginTokenCache.evict(accountsLoginConfirmationDTO.userLoginToken());
        pinVerificationCache.evict(findUser.get().getId() + "::" + accountsLoginConfirmationDTO.pin());

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/login-confirmation");
        customLinks.put("next", "/" + accountsBaseURL + "/get-profile");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_login_success",
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