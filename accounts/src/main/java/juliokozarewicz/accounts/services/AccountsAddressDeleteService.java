package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.AccountsAddressDeleteDTO;
import juliokozarewicz.accounts.exceptions.ErrorHandler;
import juliokozarewicz.accounts.persistence.entities.AccountsAddressEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsAddressRepository;
import juliokozarewicz.accounts.persistence.repositories.AccountsProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountsAddressDeleteService {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------
    @Value("${ACCOUNTS_BASE_URL}")
    private String accountsBaseURL;
    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final ErrorHandler errorHandler;
    private final AccountsAddressRepository accountsAddressRepository;
    private final CacheManager cacheManager;
    private final Cache jwtCache;

    public AccountsAddressDeleteService(

        MessageSource messageSource,
        ErrorHandler errorHandler,
        AccountsAddressRepository accountsAddressRepository,
        AccountsProfileRepository accountsProfileRepository,
        CacheManager cacheManager

    ) {

        this.messageSource = messageSource;
        this.errorHandler = errorHandler;
        this.accountsAddressRepository = accountsAddressRepository;
        this.cacheManager = cacheManager;
        this.jwtCache = cacheManager.getCache("accounts-addressCache");

    }

    // ===================================================== ( constructor end )

    // execute
    @CacheEvict(value = "accounts-addressCache", key = "#credentialsData['id']")
    public ResponseEntity execute(

        Map<String, Object> credentialsData,
        AccountsAddressDeleteDTO accountsAddressDeleteDTO

    ) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // Credentials
        UUID idUser = UUID.fromString((String) credentialsData.get("id"));

        // Credentials
        UUID idAddress = accountsAddressDeleteDTO.addressId();

        // find address
        Optional<AccountsAddressEntity> findAddress =  accountsAddressRepository
            .findByIdAndIdUser(
                idAddress,
                idUser
            );

        // Address not found
        if ( findAddress.isEmpty() ) {

            // call custom error
            errorHandler.customErrorThrow(
                404,
                messageSource.getMessage(
                    "response_address_not_found", null, locale
                )
            );

        }

        // Delete Address
        accountsAddressRepository.delete(findAddress.get());

        // Links
        Map<String, String> customLinks = new LinkedHashMap<>();
        customLinks.put("self", "/" + accountsBaseURL + "/address-delete");
        customLinks.put("next", "/" + accountsBaseURL + "/address-get");

        // Response
        StandardResponseService response = new StandardResponseService.Builder()
            .statusCode(200)
            .statusMessage("success")
            .message(
                messageSource.getMessage(
                    "response_address_deleted_success",
                    null,
                    locale
                )
            )
            .links(customLinks)
            .build();

        return ResponseEntity
            .status(response.getStatusCode())
            .body(response);

    }

}