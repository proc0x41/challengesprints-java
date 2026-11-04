package br.com.fiap.alpha4.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

/** Centraliza o DataSource. JdbcTemplate gerencia conexões e transações Spring. */
@Component
public class ConnectionFactory {
    private final JdbcTemplate template;

    public ConnectionFactory(DataSource dataSource) {
        template = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate jdbc() {
        return template;
    }
}
