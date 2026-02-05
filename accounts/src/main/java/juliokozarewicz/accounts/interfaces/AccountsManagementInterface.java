package juliokozarewicz.accounts.interfaces;

import juliokozarewicz.accounts.persistence.entities.AccountsEntity;

import java.time.Instant;
import java.util.UUID;

public interface AccountsManagementInterface {

    UUID createUniqueId();

    String createVerificationToken(UUID idUser, String email, String reason);

    String createLoginToken(UUID idUser);

    String createVerificationPin(UUID idUser, String reason, Object meta);

    String createCredentialJWT(String email);

    String createRefreshLogin(
        UUID idUser,
        String userIp,
        String userAgent,
        Instant createdAt
    );

    void deleteOneRefreshLogin(UUID idUser, String refreshToken);

    void deleteAllRefreshTokensByIdNewTransaction(UUID idUser);

    void deleteExpiredRefreshTokensListById(UUID idUser);

    void sendEmailStandard(String email, String message, String link);

    void createUserLog(
        String ipAddress,
        AccountsEntity idUser,
        String agent,
        String updateType,
        String oldValue,
        String newValue
    );

    void enableAccount(UUID idUser);

    void disableAccount(UUID idUser);

    boolean isAllowedUrl(String rawLink, String publicDomain);

}