package juliokozarewicz.accounts.services;

import juliokozarewicz.accounts.dtos.*;
import juliokozarewicz.accounts.interfaces.AccountsManagementInterface;
import juliokozarewicz.accounts.persistence.entities.AccountsEntity;
import juliokozarewicz.accounts.persistence.entities.AccountsLogEntity;
import juliokozarewicz.accounts.persistence.repositories.AccountsLogRepository;
import juliokozarewicz.accounts.persistence.repositories.AccountsRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AccountsManagementService implements AccountsManagementInterface {

    // ==================================================== ( constructor init )

    // Env
    // -------------------------------------------------------------------------

    // Attributes
    @Value("${APPLICATION_TITLE}")
    private String applicationTitle;

    // -------------------------------------------------------------------------

    private final MessageSource messageSource;
    private final AccountsRepository accountsRepository;
    private final EncryptionService encryptionService;
    private final AccountsLogRepository accountsLogRepository;
    private final UserJWTService userJWTService;
    private final AccountsKafkaService accountsKafkaService;
    private final CacheManager cacheManager;
    private final Cache arrayLoginsCache;
    private final Cache refreshLoginCache;
    private final Cache pinVerificationCache;
    private final Cache verificationCache;
    private final Cache loginTokenCache;

    public AccountsManagementService (

        MessageSource messageSource,
        EncryptionService encryptionService,
        AccountsRepository accountsRepository,
        AccountsLogRepository accountsLogRepository,
        UserJWTService userJWTService,
        AccountsKafkaService accountsKafkaService,
        CacheManager cacheManager

    ) {

        this.messageSource = messageSource;
        this.encryptionService = encryptionService;
        this.accountsRepository = accountsRepository;
        this.accountsLogRepository = accountsLogRepository;
        this.userJWTService = userJWTService;
        this.accountsKafkaService = accountsKafkaService;
        this.cacheManager = cacheManager;
        this.refreshLoginCache = cacheManager.getCache("accounts-refreshLoginCache");
        this.arrayLoginsCache = cacheManager.getCache("accounts-arrayLoginsCache");
        this.loginTokenCache = cacheManager.getCache("accounts-loginCache");
        this.pinVerificationCache = cacheManager.getCache("accounts-pinVerificationCache");
        this.verificationCache = cacheManager.getCache("accounts-verificationCache");

    }

    // ===================================================== ( constructor end )

    @Override
    public UUID createUniqueId(){

        return UuidCreator.getTimeOrderedEpoch();

    }

    @Override
    public void enableAccount(UUID idUser) {

        // Timestamp
        Instant nowUtc = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findById(
            idUser
        );

        if ( findUser.isPresent() && !findUser.get().isBanned() ) {

            // Update
            AccountsEntity user = findUser.get();
            user.setActive(true);
            user.setUpdatedAt(nowUtc);
            accountsRepository.save(user);

        }

    }

    @Override
    public void disableAccount(UUID idUser) {

        // Timestamp
        Instant nowUtc = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findById(
            idUser
        );

        if ( findUser.isPresent() && !findUser.get().isBanned() ) {

            // Update
            AccountsEntity user = findUser.get();
            user.setActive(false);
            user.setUpdatedAt(nowUtc);
            accountsRepository.save(user);

        }

    }

    @Override
    public void sendEmailStandard(String email, String message, String link) {

        // language
        Locale locale = LocaleContextHolder.getLocale();

        // send email body
        StringBuilder messageEmail = new StringBuilder();

        // Greeting
        messageEmail.append(messageSource.getMessage(
            "email_greeting", null, locale)
        ).append("<br><br>");

        // Body
        messageEmail.append(messageSource.getMessage(
            message, null, locale)
        ).append("<br><br>");

        // add link if exist
        if (link != null && !link.isEmpty()) {
            boolean isUrl = link.startsWith("http://") || link.startsWith("https://");
            messageEmail.append("<br><b>").append(

                isUrl ?

                    "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
                    "    <tr>" +
                    "        <td bgcolor=\"#000000\" " +
                    "            style=\"" +
                    "                padding:12px 20px;" +
                    "                text-align:center;" +
                    "                border-radius:8px;" +
                    "                border:3px solid #ffffff;" +
                    "            \">" +
                    "            <a href=\"" + link + "\" " +
                    "               style=\"" +
                    "                   width:100%;" +
                    "                   color:#ffffff;" +
                    "                   text-decoration:none;" +
                    "                   padding:12px 20px;" +
                    "                   font-weight:bold;" +
                    "                   font-family:Arial, sans-serif;" +
                    "               \">" +
                    messageSource.getMessage(
                        "email_click_here_link", null, locale
                    ) +
                    "            </a>" +
                    "        </td>" +
                    "    </tr>" +
                    "</table>"

                :

                "<span style=\"" +
                    "font-size:28px; " +
                    "line-height:1.5; " +
                    "letter-spacing: 3px; " +
                    "display:inline-block; " +
                    "margin-top:4px;\"" +
                ">"
                    + link +
                "</span>" +
                "<br>"

            ).append("</b><br><br>");

        }

        // Close
        messageEmail.append(
            messageSource.getMessage(
                "email_closing", null, locale
            )
        ).append("<br>").append(applicationTitle.toUpperCase());

        String subject = "[ " + applicationTitle.toUpperCase() + " ] - " + messageSource.
            getMessage(
                "email_subject_account", null, locale
            );

        // send email dto
        SendEmailDataDTO sendEmailDataDTO = new SendEmailDataDTO(
            email,
            subject,
            messageEmail.toString()
        );

        accountsKafkaService.sendSimpleEmailMessage(sendEmailDataDTO);

    }

    @Override
    public String createLoginToken(UUID idUser) {

        // Get hash
        String hashFinal = encryptionService.createToken();

        // Meta DTO
        AccountsCacheLoginConfirmationMetaDTO accountsCacheLoginConfirmationMetaDTO = new AccountsCacheLoginConfirmationMetaDTO();
        accountsCacheLoginConfirmationMetaDTO.setIdUser(idUser.toString());

        // Redis cache (hashFinal)
        loginTokenCache.put(
            hashFinal,
            accountsCacheLoginConfirmationMetaDTO
        );

        return hashFinal;

    }

    @Override
    public String createVerificationPin(
        UUID idUser,
        String reason,
        String linked,
        Object meta
    ) {

        // Create pin
        int pin = new Random().nextInt(900000) + 100000;
        String pinCode = String.valueOf(pin);

        AccountsCacheVerificationPinMetaDTO pinDTO =
            new AccountsCacheVerificationPinMetaDTO();
            pinDTO.setReason(reason);
            pinDTO.setMeta(meta);

        pinVerificationCache.put(
            idUser + "::" + pinCode,
            pinDTO
        );

        return pinCode;

    }

    @Override
    public String createVerificationToken(UUID idUser, String email, String reason) {

        // Get hash
        String hashFinal = encryptionService.createToken();

        // Verification DTO
        AccountsCacheVerificationTokenMetaDTO verificationDTO = new AccountsCacheVerificationTokenMetaDTO();
        verificationDTO.setEmail(email);
        verificationDTO.setReason(reason);

        // Redis cache (hashFinal and reason in metadata)
        verificationCache.put(
            hashFinal,
            verificationDTO
        );

        return hashFinal;

    }

    @Override
    public void createUserLog(
        String ipAddress,
        AccountsEntity user,
        String agent,
        String updateType,
        String oldValue,
        String newValue
    ) {

        // ID and Timestamp
        Instant nowUtc = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        // Create log
        AccountsLogEntity newUserLog = new AccountsLogEntity();
        newUserLog.setId(createUniqueId());
        newUserLog.setCreatedAt(nowUtc);
        newUserLog.setIpAddress(ipAddress);
        newUserLog.setAgent(agent);
        newUserLog.setUpdateType(updateType);
        newUserLog.setOldValue(oldValue);
        newUserLog.setNewValue(newValue);
        newUserLog.setUser(user);
        accountsLogRepository.save(newUserLog);

    }

    @Override
    public String createCredentialJWT(String email) {

        // find user
        Optional<AccountsEntity> findUser =  accountsRepository.findByEmail(
            email.toLowerCase()
        );

        // Payload
        Map<String, String> credentialPayload = new LinkedHashMap<>();
        credentialPayload.put("id", findUser.get().getId().toString());
        credentialPayload.put("email", findUser.get().getEmail());
        credentialPayload.put("level", findUser.get().getLevel());

        // Create raw JWT
        String credentialsTokenRaw = userJWTService.createCredential(
            credentialPayload
        );

        return encryptionService.encrypt(credentialsTokenRaw);

    }

    @Override
    public String createRefreshLogin(
        UUID idUser,
        String userIp,
        String userAgent,
        Instant createdAt
    ) {

        // Timestamp
        Instant nowUtc = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        // Create raw refresh token
        String hashFinal = encryptionService.createToken();

        // Encrypt refresh token
        String encryptedRefreshToken = encryptionService.encrypt(
            hashFinal
        );

        // Redis cache token -> data
        // ---------------------------------------------------------------------
        Instant newCreatedAt = createdAt == null
            ? nowUtc
            : createdAt;

        AccountsCacheRefreshTokenDTO dtoRefreshToken = new
            AccountsCacheRefreshTokenDTO(
                idUser,
                userIp,
                userAgent,
                newCreatedAt
            );

        refreshLoginCache.put(
            encryptedRefreshToken,
            dtoRefreshToken
        );

        // Redis cache idUser -> tokens
        // ---------------------------------------------------------------------
        AccountsCacheRefreshTokensListDTO tokensDTO = arrayLoginsCache.get(
            idUser,
            AccountsCacheRefreshTokensListDTO.class
        );

        List<AccountsCacheRefreshTokensListMetaDTO> refreshTokensList;

        refreshTokensList = (tokensDTO == null || tokensDTO.getRefreshTokensActive() == null)
            ? new ArrayList<>()
            : new ArrayList<>(tokensDTO.getRefreshTokensActive());

        // Create metadata for the new token (including the timestamp)
        AccountsCacheRefreshTokensListMetaDTO newTokenMeta =
            new AccountsCacheRefreshTokensListMetaDTO(
            nowUtc, // Timestamp
            encryptedRefreshToken
        );

        // Add the new token metadata to the list
        refreshTokensList.add(newTokenMeta);

        // Update the cache with the new list of tokens and metadata
        AccountsCacheRefreshTokensListDTO updatedDTO =
            new AccountsCacheRefreshTokensListDTO(refreshTokensList);

        arrayLoginsCache.put(idUser, updatedDTO);

        // --------------------------------------------------------------------

        return encryptedRefreshToken;

    }

    @Override
    public void deleteOneRefreshLogin(UUID idUser, String refreshToken) {

        refreshLoginCache.evict(refreshToken);

        // Get user's tokens with metadata
        AccountsCacheRefreshTokensListDTO tokensDTO = arrayLoginsCache.get(
            idUser,
            AccountsCacheRefreshTokensListDTO.class
        );

        if (
            tokensDTO == null || tokensDTO.getRefreshTokensActive() == null
        ) return;

        // Convert list of tokens with metadata to a mutable list
        List<AccountsCacheRefreshTokensListMetaDTO> tokensList = new ArrayList<>(
            tokensDTO.getRefreshTokensActive()
        );

        // Remove token from list
        boolean removed = tokensList.removeIf(
            tokenMeta -> tokenMeta
            .getRefreshToken().equals(refreshToken)
        );

        if (!removed) return; // Token not found, so we exit

        // Update the cache with the remaining tokens
        AccountsCacheRefreshTokensListDTO updatedDTO =
            new AccountsCacheRefreshTokensListDTO(tokensList);

        arrayLoginsCache.put(idUser, updatedDTO);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllRefreshTokensByIdNewTransaction(UUID idUser) {

        // Recover all tokens by user id
        AccountsCacheRefreshTokensListDTO tokensDTO = arrayLoginsCache.get(
            idUser,
            AccountsCacheRefreshTokensListDTO.class
        );

        if (
            tokensDTO == null ||
            tokensDTO.getRefreshTokensActive() == null
        ) return;

        // Revoke all tokens by iterating over the metadata list
        for (
            AccountsCacheRefreshTokensListMetaDTO tokenMeta :
            tokensDTO.getRefreshTokensActive()
        ) {
            // Using the refreshToken from the metadata to delete it
            deleteOneRefreshLogin(idUser, tokenMeta.getRefreshToken());
        }

        // Evict user from the cache after revoking all tokens
        arrayLoginsCache.evict(idUser);

    }

    @Override
    public void deleteExpiredRefreshTokensListById(UUID idUser) {

        // Retrieve all active tokens from the cache for the given user
        AccountsCacheRefreshTokensListDTO tokensDTO = arrayLoginsCache.get(
            idUser,
            AccountsCacheRefreshTokensListDTO.class
        );

        // If no tokens exist or the list is empty, return early
        if (tokensDTO == null || tokensDTO.getRefreshTokensActive() == null) return;

        // Retrieve the list of tokens with metadata
        List<AccountsCacheRefreshTokensListMetaDTO> tokensList =
            new ArrayList<>(tokensDTO.getRefreshTokensActive());

        // Remove expired tokens (those older than 16 days)
        Instant fifteenDaysAgo = Instant.now().minus(15, ChronoUnit.DAYS);

        tokensList.removeIf(tokenMeta ->
            !tokenMeta.getTimestamp().isAfter(fifteenDaysAgo));

        // Update the cache with the filtered list of tokens
        AccountsCacheRefreshTokensListDTO updatedDTO =
            new AccountsCacheRefreshTokensListDTO(tokensList);

        arrayLoginsCache.put(idUser, updatedDTO);

    }

    @Override
    public boolean isAllowedUrl(String rawLink, String publicDomain) {

        try {
            if (rawLink == null || rawLink.isBlank()) return false;

            String link = rawLink.trim();
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                link = "https://" + link;
            }

            URI uri = new URI(link);

            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getFragment() != null) return false;

            String host = uri.getHost();
            if (host == null) return false;
            final String finalHost = host.toLowerCase();

            String query = uri.getQuery();
            if (query != null) {
                String q = query.toLowerCase();
                if (
                    q.contains("redirect=") ||
                        q.contains("next=") ||
                        q.contains("url=") ||
                        q.contains("return=") ||
                        q.contains("continue=") ||
                        q.contains("target=")
                ) return false;
            }

            boolean allowed = Arrays.stream(publicDomain.split(","))
                .map(String::trim)
                .map(d -> d.startsWith("http") ? d : "https://" + d)
                .map(d -> {
                    try { return new URI(d).getHost(); }
                    catch (Exception e) { return null; }
                })
                .anyMatch(h -> h != null && h.equalsIgnoreCase(finalHost));

            if (!allowed) return false;

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int status = conn.getResponseCode();
            return status < 300 || status >= 400;

        } catch (Exception e) {
            return false;
        }

    }

}