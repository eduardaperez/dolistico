package juliokozarewicz.accounts.services;

import jakarta.transaction.Transactional;
import juliokozarewicz.accounts.dtos.AccountsActivateDTO;
import juliokozarewicz.accounts.dtos.AccountsCacheVerificationTokenMetaDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsLogRepository;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountsActivateService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final AccountsManagementService accountsManagementService;
    private final ErrorHandler errorHandler;
    private final AccountsRepository accountsRepository;
    private final CacheManager cacheManager;
    private final Cache verificationCache;
    private final Cache notActivatedAccountCache;

    public AccountsActivateService (

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
        this.cacheManager = cacheManager;
        this.verificationCache = cacheManager.getCache("accounts-verificationCache");
        this.notActivatedAccountCache = cacheManager.getCache("accounts-notActivatedAccountCache");

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        String userIp,
        String userAgent,
        AccountsActivateDTO accountsActivateDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // find token
        AccountsCacheVerificationTokenMetaDTO findToken = Optional
            .ofNullable(verificationCache.get(accountsActivateDTO.token()))
            .map(Cache.ValueWrapper::get)
            .map(AccountsCacheVerificationTokenMetaDTO.class::cast)
            .filter(
                tokenMeta -> tokenMeta
                    .getReason().equals(AccountsUpdateEnum.ACTIVATE_ACCOUNT)
            )
            .orElse(null);

        // token not exist
        if ( findToken == null ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_activate_account_error", null, locale
                )
            );

        }

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            findToken.getEmail()
        );

        // account not exist
        if ( findUser.isEmpty() ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_activate_account_error", null, locale
                )
            );

        }

        // account already activated
        if ( findUser.get().isActive() || findUser.get().isBanned() ) {

            // Revoke current token
            verificationCache.evict(accountsActivateDTO.token());

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_activate_account_error", null, locale
                )
            );

        }

        // Active account
        if ( !findUser.get().isActive() && !findUser.get().isBanned() ) {

            // Update user log
            accountsManagementService.createUserLog(
                userIp,
                findUser.get(),
                userAgent,
                AccountsUpdateEnum.ACTIVATE_ACCOUNT,
                String.valueOf(findUser.get().isActive()),
                "true"
            );

            // active account in database
            accountsManagementService.enableAccount(findUser.get().getId());

            // Evict not activated account cache
            notActivatedAccountCache.evict(findUser.get().getId());

        }

        // Revoke current token
        verificationCache.evict(accountsActivateDTO.token());

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/activate-email");
        customLinks.put("next", "/" + accountsBaseURL + "/login");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_account_activate",
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