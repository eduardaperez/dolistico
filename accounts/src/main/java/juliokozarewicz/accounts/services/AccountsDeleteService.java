package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsCacheVerificationTokenMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsDeleteDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
import juliokozarewicz.accounts.enums.EmailResponsesEnum;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AccountsDeleteService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final AccountsManagementService accountsManagementService;
    private final AccountsRepository accountsRepository;
    private final ErrorHandler errorHandler;
    private final Cache deletedAccountByUserCache;
    private final Cache verificationCache;
    private final Cache profileCache;
    private final Cache addressCache;

    public AccountsDeleteService(

        MessageSource messageSource,
        AccountsManagementService accountsManagementService,
        AccountsRepository accountsRepository,
        ErrorHandler errorHandler,
        CacheManager cacheManager

    ) {

        this.messageSource = messageSource;
        this.accountsManagementService = accountsManagementService;
        this.accountsRepository = accountsRepository;
        this.errorHandler = errorHandler;
        this.deletedAccountByUserCache = cacheManager.getCache("accounts-deletedAccountByUserCache");
        this.verificationCache = cacheManager.getCache("accounts-verificationCache");
        this.addressCache = cacheManager.getCache("accounts-addressCache");
        this.profileCache = cacheManager.getCache("accounts-profileCache");

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        String userIp,
        String userAgent,
        AccountsDeleteDTO accountsDeleteDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // process to delete account
        // ---------------------------------------------------------------------

        // find token
        AccountsCacheVerificationTokenMetaDTO findToken = Optional
            .ofNullable(verificationCache.get(accountsDeleteDTO.token()))
            .map(Cache.ValueWrapper::get)
            .map(AccountsCacheVerificationTokenMetaDTO.class::cast)
            .filter(
                tokenMeta -> tokenMeta
                    .getReason().equals(AccountsUpdateEnum.DELETE_ACCOUNT)
            )
            .orElse(null);

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            findToken.getEmail()
        );

        // User not exist
        if ( findUser.isEmpty() ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_delete_account_error", null, locale
                )
            );

        }

        // Deactivate account
        accountsManagementService.disableAccount(findUser.get().getId());

        // Create user log
        accountsManagementService.createUserLog(
            userIp,
            findUser.get(),
            userAgent,
            AccountsUpdateEnum.DELETE_ACCOUNT,
            "activated",
            "deleted"
        );

        // Set delete account cache
        deletedAccountByUserCache.put(
            findUser.get().getId(),
            ZonedDateTime.now(ZoneOffset.UTC).toInstant()
        );

        // Revoke user data
        profileCache.evict(findUser.get().getId());
        addressCache.evict(findUser.get().getId());

        // Revoke all refresh tokens
        accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
            findUser.get().getId()
        );

        // Clean expired refresh tokens
        accountsManagementService.deleteExpiredRefreshTokensListById(
            findUser.get().getId()
        );

        // send email
        accountsManagementService.sendEmailStandard(
            findUser.get().getEmail(),
            EmailResponsesEnum.ACCOUNT_DELETED_TIME,
            null
        );

        // Revoke current token
        verificationCache.evict(accountsDeleteDTO.token());

        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/delete");
        customLinks.put("next", "/" + accountsBaseURL + "/signup");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_delete_account_success",
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