package br.com.fiap.alpha4.bo;

import br.com.fiap.alpha4.dao.ParticipantDao;
import br.com.fiap.alpha4.exception.ApiException;
import br.com.fiap.alpha4.model.Participant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SessionBo {
    private final ParticipantDao participants;
    private final String adminKey;
    private final SecureRandom random = new SecureRandom();

    public SessionBo(ParticipantDao participants, @Value("${app.admin-key}") String adminKey) {
        this.participants = participants;
        this.adminKey = adminKey;
    }

    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    public Participant requireParticipant(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 512) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Crie ou recupere sua sessão para continuar.");
        }
        return participants.findByToken(hash(authorization.substring(7)))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Crie uma nova sessão."));
    }

    public Participant optionalParticipant(String authorization) {
        return authorization == null ? null : requireParticipant(authorization);
    }

    public void requireAdmin(String providedKey) {
        if (adminKey.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A administração não foi configurada neste ambiente.");
        if (providedKey == null || !MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acesso administrativo não autorizado.");
        }
    }
}
