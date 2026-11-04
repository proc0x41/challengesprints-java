package br.com.fiap.alpha4.resource;

import br.com.fiap.alpha4.config.ConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthResource {
    private final ConnectionFactory factory;
    public HealthResource(ConnectionFactory factory) { this.factory = factory; }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        factory.jdbc().queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "ok", "service", "alpha4-soul-up");
    }
}
