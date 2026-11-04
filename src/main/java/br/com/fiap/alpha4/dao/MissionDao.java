package br.com.fiap.alpha4.dao;

import br.com.fiap.alpha4.config.ConnectionFactory;
import br.com.fiap.alpha4.model.Category;
import br.com.fiap.alpha4.model.Mission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class MissionDao {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Mission> MAPPER = (rs, row) -> new Mission(
        rs.getString("id"), rs.getString("title"), rs.getString("description"),
        Category.from(rs.getString("category")), rs.getInt("points"), rs.getBoolean("requires_evidence"), rs.getString("mission_status"));
    private static final String SELECT = """
        SELECT m.*, COALESCE((SELECT CASE e.status
          WHEN 'aprovada' THEN 'concluida' WHEN 'em-analise' THEN 'em-analise' ELSE 'disponivel' END
          FROM evidence e WHERE e.mission_id=m.id AND e.participant_id=? AND e.cycle_day=?), 'disponivel') AS mission_status
        FROM missions m
        """;

    public MissionDao(ConnectionFactory factory) { jdbc = factory.jdbc(); }

    public List<Mission> list(String participantId, LocalDate day) {
        return jdbc.query(SELECT + " ORDER BY m.title,m.id", MAPPER, participantId, day);
    }

    public Optional<Mission> find(String id, String participantId, LocalDate day) {
        return jdbc.query(SELECT + " WHERE m.id=?", MAPPER, participantId, day, id).stream().findFirst();
    }

    public void insert(Mission mission) {
        jdbc.update("INSERT INTO missions(id,title,description,category,points,requires_evidence) VALUES(?,?,?,?,?,?)",
            mission.id(), mission.title(), mission.description(), mission.category().value(), mission.points(), mission.requiresEvidence());
    }

    public void update(Mission mission) {
        jdbc.update("UPDATE missions SET title=?,description=?,category=?,points=?,requires_evidence=? WHERE id=?",
            mission.title(), mission.description(), mission.category().value(), mission.points(), mission.requiresEvidence(), mission.id());
    }

    public void delete(String id) { jdbc.update("DELETE FROM missions WHERE id=?", id); }
    public int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM missions", Integer.class); }
}
