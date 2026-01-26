package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsCacheVerificationPinMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsCacheVerificationTokenMetaDTO;
import juliokozarewicz.accounts.dtos.AccountsUpdateEmailDTO;
import juliokozarewicz.accounts.enums.AccountsUpdateEnum;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AccountsUpdateEmailService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final AccountsManagementService accountsManagementService;
    private final EncryptionService encryptionService;
    private final AccountsRepository accountsRepository;
    private final ErrorHandler errorHandler;
    private final Cache pinVerificationCache;
    private final Cache verificationCache;

    public AccountsUpdateEmailService(

        MessageSource messageSource,
        AccountsManagementService accountsManagementService,
        EncryptionService encryptionService,
        AccountsRepository accountsRepository,
        ErrorHandler errorHandler,
        CacheManager cacheManager

    ) {

        this.messageSource = messageSource;
        this.accountsManagementService = accountsManagementService;
        this.encryptionService = encryptionService;
        this.accountsRepository = accountsRepository;
        this.errorHandler = errorHandler;
        this.pinVerificationCache = cacheManager.getCache("accounts-pinVerificationCache");
        this.verificationCache = cacheManager.getCache("accounts-verificationCache");

    }

    // ===================================================== ( constructor end )

    @Transactional
    public ResponseEntity execute(

        String userIp,
        String userAgent,
        Map<String, Object> credentialsData,
        AccountsUpdateEmailDTO accountsUpdateEmailDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // Credentials
        UUID idUser = UUID.fromString((String) credentialsData.get("id"));

        // process to change email
        // ---------------------------------------------------------------------

        // find user
        Optional<AccountsEntity> findOldUser =  accountsRepository.findById(
            idUser
        );

        // Timestamp
        Instant nowUtc = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        if ( findOldUser.isEmpty() ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_update_email_fail", null, locale
                )
            );

        }

        // find old email and pin
        AccountsCacheVerificationPinMetaDTO pinDTO = null;
        String pinLoginCacheKey = findOldUser.get().getId() + "::" + accountsUpdateEmailDTO.pin();
        pinDTO = Optional
            .ofNullable(pinVerificationCache.get(pinLoginCacheKey))
            .map(Cache.ValueWrapper::get)
            .map(AccountsCacheVerificationPinMetaDTO.class::cast)
            .filter(
                tokenMeta -> tokenMeta
                .getReason().equals(AccountsUpdateEnum.UPDATE_EMAIL)
            )
            .orElse(null);

        // find old email and token
        AccountsCacheVerificationTokenMetaDTO findOldEmailAndToken = null;
        findOldEmailAndToken = Optional
            .ofNullable(verificationCache.get(findOldUser.get().getId()))
            .map(Cache.ValueWrapper::get)
            .map(AccountsCacheVerificationTokenMetaDTO.class::cast)
            .filter(
                tokenMeta -> accountsUpdateEmailDTO
                    .token().equals(tokenMeta.getVerificationToken())
            )
            .filter(
                tokenMeta -> tokenMeta
                    .getReason().equals(AccountsUpdateEnum.UPDATE_EMAIL)
            )
            .orElse(null);


        // If token or email not found return error
        if ( pinDTO == null || findOldEmailAndToken == null ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_update_email_fail", null, locale
                )
            );

        }

        // Decoded new email
        String decodedNewEmail = String.valueOf(pinDTO.getMeta());

        // If new email exist return error
        Optional<AccountsEntity> findNewUser =  accountsRepository.findByEmail(
            String.valueOf(pinDTO.getMeta())
        );

        if ( findNewUser.isPresent() ) {

            // call custom error
            errorHandler.customErrorThrow(
                409,
                messageSource.getMessage(
                    "response_update_email_fail", null, locale
                )
            );

        }

        // Create user log
        accountsManagementService.createUserLog(
            userIp,
            findOldUser.get().getId(),
            userAgent,
            AccountsUpdateEnum.UPDATE_EMAIL,
            findOldUser.get().getEmail(),
            decodedNewEmail
        );

        // Update database with new email
        findOldUser.get().setEmail(decodedNewEmail);
        findOldUser.get().setUpdatedAt(nowUtc);
        accountsRepository.save(findOldUser.get());

        // Revoke all refresh tokens
        accountsManagementService.deleteAllRefreshTokensByIdNewTransaction(
            findOldUser.get().getId()
        );

        // Delete all verification tokens
        accountsManagementService.deleteAllVerificationTokenByIdUserNewTransaction(
            findOldUser.get().getId()
        );

        // Clean expired refresh tokens
        accountsManagementService.deleteExpiredRefreshTokensListById(
            findOldUser.get().getId()
        );

        // Revoke current pin
        pinVerificationCache.evict(pinLoginCacheKey);
        // ---------------------------------------------------------------------

        // Response
        // ---------------------------------------------------------------------

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/update-email");
        customLinks.put("next", "/" + accountsBaseURL + "/login");

        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_update_email_success",
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