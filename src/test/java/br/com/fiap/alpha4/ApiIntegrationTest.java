package br.com.fiap.alpha4;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "app.admin-key=admin-key-only-for-automated-tests", "app.seed-demo=false",
    "CORS_ORIGINS=http://localhost:5173,http://127.0.0.1:5173"
})
class ApiIntegrationTest {
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;
    static final String ADMIN = "admin-key-only-for-automated-tests";

    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        final AtomicReference<Instant> instant = new AtomicReference<>();
        void set(String value) { instant.set(Instant.parse(value)); }
        @Override public ZoneId getZone() { return ZoneId.of("America/Sao_Paulo"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return instant.get(); }
    }

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM friendships");
        jdbc.update("DELETE FROM evidence");
        jdbc.update("DELETE FROM missions");
        jdbc.update("DELETE FROM participants");
        clock.set("2026-11-03T15:00:00Z");
    }

    ResponseEntity<JsonNode> request(HttpMethod method, String path, Object body, String token, boolean admin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        if (admin) headers.set("X-Admin-Key", ADMIN);
        return rest.exchange("/api" + path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    JsonNode profile(String name, String uf, String community) {
        var response = request(HttpMethod.POST, "/participantes", Map.of("name", name, "uf", uf, "community", community), null, false);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return response.getBody();
    }

    Map<String, Object> missionBody(int points) {
        return Map.of("title", "Missão para teste", "description", "Separar materiais de descarte conforme orientações municipais.", "category", "ambiental", "points", points, "requiresEvidence", true);
    }

    String mission(int points) {
        var response = request(HttpMethod.POST, "/missoes", missionBody(points), null, true);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return response.getBody().path("id").asText();
    }

    @Test
    void missionCrudRequiresAdminAndPreservesHttpStatus() {
        assertThat(request(HttpMethod.POST, "/missoes", missionBody(20), null, false).getStatusCode().value()).isEqualTo(403);
        String id = mission(20);
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, null, false).getBody().path("status").asText()).isEqualTo("disponivel");
        var updated = request(HttpMethod.PUT, "/missoes/" + id, missionBody(35), null, true);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody().path("points").asInt()).isEqualTo(35);
        assertThat(request(HttpMethod.DELETE, "/missoes/" + id, null, null, true).getStatusCode().value()).isEqualTo(204);
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, null, false).getStatusCode().value()).isEqualTo(404);
        assertThat(request(HttpMethod.PUT, "/missoes/ausente", missionBody(35), null, true).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void profileCrudUsesHashedTokenAndNormalizesOptionalCommunity() {
        var response = request(HttpMethod.POST, "/participantes", Map.of("name", "  Ab  ", "uf", "SP"), null, false);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode session = response.getBody();
        String token = session.path("token").asText();
        assertThat(session.path("participant").path("community").asText()).isEmpty();
        assertThat(session.path("participant").path("name").asText()).isEqualTo("Ab");
        String stored = jdbc.queryForObject("SELECT token_hash FROM participants", String.class);
        assertThat(stored).hasSize(64).isNotEqualTo(token);
        assertThat(request(HttpMethod.GET, "/participantes/me", null, null, false).getStatusCode().value()).isEqualTo(401);
        var updated = request(HttpMethod.PUT, "/participantes/me", Map.of("name", "Novo pseudônimo", "uf", "RJ", "community", "Grupo real"), token, false);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody().path("uf").asText()).isEqualTo("RJ");
        assertThat(request(HttpMethod.DELETE, "/participantes/me", null, token, false).getStatusCode().value()).isEqualTo(204);
        assertThat(request(HttpMethod.GET, "/participantes/me", null, token, false).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void approvalCreditsSnapshotOnceAndResetsMissionAtSaoPauloMidnight() {
        String token = profile("Participante teste", "SP", "Alpha4").path("token").asText();
        String id = mission(20);
        assertThat(request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "Realizei a separação dos materiais."), token, false).getStatusCode().value()).isEqualTo(201);
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, token, false).getBody().path("status").asText()).isEqualTo("em-analise");
        assertThat(request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "Envio duplicado da participação."), token, false).getStatusCode().value()).isEqualTo(409);
        String evidenceId = request(HttpMethod.GET, "/admin/evidencias", null, null, true).getBody().get(0).path("id").asText();
        request(HttpMethod.PUT, "/missoes/" + id, missionBody(100), null, true);
        assertThat(request(HttpMethod.PUT, "/admin/evidencias/" + evidenceId, Map.of("status", "aprovada"), null, true).getStatusCode().value()).isEqualTo(200);
        assertThat(request(HttpMethod.PUT, "/admin/evidencias/" + evidenceId, Map.of("status", "aprovada"), null, true).getStatusCode().value()).isEqualTo(409);
        assertThat(request(HttpMethod.GET, "/ranking", null, null, false).getBody().path("entries").get(0).path("points").asInt()).isEqualTo(20);
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, token, false).getBody().path("status").asText()).isEqualTo("concluida");
        assertThat(request(HttpMethod.DELETE, "/missoes/" + id, null, null, true).getStatusCode().value()).isEqualTo(409);
        clock.set("2026-11-04T02:59:59Z"); // ainda dia 03 em São Paulo
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, token, false).getBody().path("status").asText()).isEqualTo("concluida");
        clock.set("2026-11-04T03:00:00Z");
        assertThat(request(HttpMethod.GET, "/missoes/" + id, null, token, false).getBody().path("status").asText()).isEqualTo("disponivel");
        assertThat(request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "Nova participação em outro dia."), token, false).getStatusCode().value()).isEqualTo(201);
        clock.set("2027-01-01T03:00:00Z");
        assertThat(request(HttpMethod.GET, "/ranking", null, null, false).getBody().path("entries").get(0).path("points").asInt()).isZero();
    }

    @Test
    void rejectionAllowsCorrectionWithoutDuplicatingDailyEvidence() {
        String token = profile("Participante teste", "SP", "Alpha4").path("token").asText();
        String missionId = mission(20);
        String path = "/missoes/" + missionId + "/evidencias";
        request(HttpMethod.POST, path, Map.of("text", "Minha descrição precisa de revisão."), token, false);
        String evidenceId = request(HttpMethod.GET, "/admin/evidencias", null, null, true).getBody().get(0).path("id").asText();
        request(HttpMethod.PUT, "/admin/evidencias/" + evidenceId, Map.of("status", "rejeitada"), null, true);
        assertThat(request(HttpMethod.GET, "/missoes/" + missionId, null, token, false).getBody().path("status").asText()).isEqualTo("disponivel");
        assertThat(request(HttpMethod.POST, path, Map.of("text", "Descrição corrigida com a ação realizada."), token, false).getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidence", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SUM(awarded_points) FROM evidence", Integer.class)).isZero();
    }

    @Test
    void rankingScopesUseOnlyPersistedRelationships() {
        JsonNode current = profile("Participante atual", "SP", "Alpha4");
        String token = current.path("token").asText();
        String friend = profile("Amigo acompanhado", "RJ", "Outro grupo").path("participant").path("id").asText();
        profile("Outro participante", "SP", "Alpha4");
        assertThat(request(HttpMethod.GET, "/ranking?scope=estadual", null, null, false).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.GET, "/ranking?scope=estadual", null, token, false).getBody().path("entries").size()).isEqualTo(2);
        assertThat(request(HttpMethod.GET, "/ranking?scope=comunidade", null, token, false).getBody().path("entries").size()).isEqualTo(2);
        assertThat(request(HttpMethod.GET, "/ranking?scope=amigos", null, token, false).getBody().path("entries").size()).isEqualTo(1);
        assertThat(request(HttpMethod.POST, "/amigos/" + friend, null, token, false).getStatusCode().value()).isEqualTo(201);
        assertThat(request(HttpMethod.GET, "/ranking?scope=amigos", null, token, false).getBody().path("entries").size()).isEqualTo(2);
        assertThat(request(HttpMethod.POST, "/amigos/" + friend, null, token, false).getStatusCode().value()).isEqualTo(409);
        assertThat(request(HttpMethod.POST, "/amigos/" + current.path("participant").path("id").asText(), null, token, false).getStatusCode().value()).isEqualTo(400);
        assertThat(request(HttpMethod.DELETE, "/amigos/" + friend, null, token, false).getStatusCode().value()).isEqualTo(204);
        assertThat(request(HttpMethod.GET, "/amigos", null, token, false).getBody().size()).isZero();
        String noGroup = profile("Sem comunidade", "SP", "").path("token").asText();
        assertThat(request(HttpMethod.GET, "/ranking?scope=comunidade", null, noGroup, false).getBody().path("entries").size()).isZero();
    }

    @Test
    void invalidInputsAndTokensNeverBecomeSuccess() {
        assertThat(request(HttpMethod.POST, "/participantes", Map.of("name", "Inválido", "uf", "ZZ"), null, false).getStatusCode().value()).isEqualTo(400);
        assertThat(request(HttpMethod.POST, "/participantes", Map.of("name", "X", "uf", "SP"), null, false).getStatusCode().value()).isEqualTo(400);
        assertThat(request(HttpMethod.POST, "/missoes", Map.of("title", "X", "points", -1), null, true).getStatusCode().value()).isEqualTo(400);
        assertThat(request(HttpMethod.GET, "/missoes", null, "invalid-token", false).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.GET, "/ranking?scope=invalido", null, null, false).getStatusCode().value()).isEqualTo(400);
        String id = mission(20);
        assertThat(request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "Uma descrição válida."), null, false).getStatusCode().value()).isEqualTo(401);
        String token = profile("Teste de entrada", "SP", "").path("token").asText();
        assertThat(request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "          a          "), token, false).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void concurrentSubmissionsAndReviewsCannotDuplicateCredit() throws Exception {
        String token = profile("Teste concorrente", "SP", "Alpha4").path("token").asText();
        String id = mission(20);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> submit = () -> request(HttpMethod.POST, "/missoes/" + id + "/evidencias", Map.of("text", "Participação enviada simultaneamente."), token, false).getStatusCode().value();
            var submissions = executor.invokeAll(List.of(submit, submit));
            assertThat(List.of(submissions.get(0).get(), submissions.get(1).get())).containsExactlyInAnyOrder(201, 409);
            String evidenceId = request(HttpMethod.GET, "/admin/evidencias", null, null, true).getBody().get(0).path("id").asText();
            Callable<Integer> review = () -> request(HttpMethod.PUT, "/admin/evidencias/" + evidenceId, Map.of("status", "aprovada"), null, true).getStatusCode().value();
            var reviews = executor.invokeAll(List.of(review, review));
            assertThat(List.of(reviews.get(0).get(), reviews.get(1).get())).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbc.queryForObject("SELECT SUM(awarded_points) FROM evidence", Integer.class)).isEqualTo(20);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void participantLimitsAreAppliedAfterTrimming() {
        var accepted = request(HttpMethod.POST, "/participantes",
            Map.of("name", "  " + "A".repeat(80) + "  ", "uf", "SP", "community", "  " + "B".repeat(80) + "  "), null, false);
        assertThat(accepted.getStatusCode().value()).isEqualTo(201);
        assertThat(accepted.getBody().path("participant").path("name").asText()).hasSize(80);
        assertThat(accepted.getBody().path("participant").path("community").asText()).hasSize(80);
        assertThat(request(HttpMethod.POST, "/participantes", Map.of("name", "A".repeat(81), "uf", "SP"), null, false).getStatusCode().value()).isEqualTo(400);
        assertThat(request(HttpMethod.POST, "/participantes", Map.of("name", "Participante", "uf", "SP", "community", "B".repeat(81)), null, false).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void rejectsScalarCoercionInsteadOfSilentlyChangingJsonTypes() {
        for (Object invalidPoints : List.of(20.9, 20.0, "20", true)) {
            Map<String, Object> body = new HashMap<>(missionBody(20));
            body.put("points", invalidPoints);
            assertThat(request(HttpMethod.POST, "/missoes", body, null, true).getStatusCode().value())
                .as("points deve ser inteiro JSON: %s", invalidPoints).isEqualTo(400);
        }
        for (Object invalidBoolean : List.of("false", "true", 0, 1)) {
            Map<String, Object> body = new HashMap<>(missionBody(20));
            body.put("requiresEvidence", invalidBoolean);
            assertThat(request(HttpMethod.POST, "/missoes", body, null, true).getStatusCode().value())
                .as("requiresEvidence deve ser boolean JSON: %s", invalidBoolean).isEqualTo(400);
        }
        for (String field : List.of("name", "community", "uf")) {
            for (Object invalidString : List.of(123, 20.9, true)) {
                Map<String, Object> body = new HashMap<>(Map.of("name", "Participante", "uf", "SP", "community", "Alpha4"));
                body.put(field, invalidString);
                assertThat(request(HttpMethod.POST, "/participantes", body, null, false).getStatusCode().value())
                    .as("%s deve ser string JSON: %s", field, invalidString).isEqualTo(400);
            }
        }
        Map<String, Object> valid = new HashMap<>(missionBody(20));
        valid.put("requiresEvidence", false);
        assertThat(request(HttpMethod.POST, "/missoes", valid, null, true).getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participants", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM missions", Integer.class)).isEqualTo(1);
    }

    @Test
    void missingResourcesAndUnsupportedRequestsKeepHttpSemantics() {
        assertThat(request(HttpMethod.GET, "/nao-encontrado", null, null, false).getStatusCode().value()).isEqualTo(404);
        assertThat(request(HttpMethod.POST, "/ranking", Map.of(), null, false).getStatusCode().value()).isEqualTo(405);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        var response = rest.exchange("/api/participantes", HttpMethod.POST,
            new HttpEntity<>("texto sem JSON", headers), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(415);
        assertThat(response.getBody().path("message").asText()).doesNotContain("Exception", "SELECT", "token_hash");
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        var allowed = rest.exchange("/api/participantes", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
        assertThat(allowed.getStatusCode().value()).isEqualTo(204);
        assertThat(allowed.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
        headers.setOrigin("https://chellengesprints-alpha4.vercel.app");
        headers.setAccessControlRequestHeaders(List.of("Content-Type", "Authorization"));
        var production = rest.exchange("/api/participantes", HttpMethod.OPTIONS,
            new HttpEntity<>(headers), String.class);
        assertThat(production.getStatusCode().value()).isEqualTo(204);
        assertThat(production.getHeaders().getAccessControlAllowOrigin())
            .isEqualTo("https://chellengesprints-alpha4.vercel.app");
        assertThat(production.getHeaders().getAccessControlAllowHeaders())
            .contains("Content-Type", "Authorization");
        var missions = rest.exchange("/api/missoes", HttpMethod.GET,
            new HttpEntity<>(headers), String.class);
        assertThat(missions.getStatusCode().value()).isEqualTo(200);
        assertThat(missions.getHeaders().getAccessControlAllowOrigin())
            .isEqualTo("https://chellengesprints-alpha4.vercel.app");
        headers.setOrigin("https://chellengesprints-alpha4.vercel.app.evil.example");
        assertThat(rest.exchange("/api/participantes", HttpMethod.OPTIONS,
            new HttpEntity<>(headers), String.class).getStatusCode().value()).isEqualTo(403);
        headers.setOrigin("https://origem-nao-autorizada.example");
        assertThat(rest.exchange("/api/participantes", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class).getStatusCode().value()).isEqualTo(403);
        assertThat(request(HttpMethod.GET, "/health", null, null, false).getBody().path("status").asText()).isEqualTo("ok");
    }
}
