package br.com.fiap.alpha4.dao;

import br.com.fiap.alpha4.config.ConnectionFactory;
import br.com.fiap.alpha4.model.Evidence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class EvidenceDao {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Evidence> MAPPER = (rs, row) -> new Evidence(
        rs.getString("id"), rs.getString("mission_id"), rs.getString("mission_title"),
        rs.getString("participant_id"), rs.getString("evidence_text"), rs.getString("status"),
        rs.getObject("cycle_day", LocalDate.class), rs.getObject("submitted_at", OffsetDateTime.class),
        rs.getObject("reviewed_at", OffsetDateTime.class), rs.getInt("points_snapshot"), rs.getInt("awarded_points"));
    private static final String SELECT = "SELECT e.*, m.title AS mission_title FROM evidence e JOIN missions m ON m.id=e.mission_id";

    public EvidenceDao(ConnectionFactory factory) { jdbc = factory.jdbc(); }

    public Optional<Evidence> findToday(String missionId, String participantId, LocalDate day) {
        return jdbc.query(SELECT + " WHERE e.mission_id=? AND e.participant_id=? AND e.cycle_day=?", MAPPER, missionId, participantId, day).stream().findFirst();
    }

    public Optional<Evidence> lock(String id) {
        return jdbc.query(SELECT + " WHERE e.id=? FOR UPDATE", MAPPER, id).stream().findFirst();
    }

    public List<Evidence> own(String participantId) {
        return jdbc.query(SELECT + " WHERE e.participant_id=? ORDER BY e.submitted_at DESC,e.id", MAPPER, participantId);
    }

    public List<Evidence> pending() {
        return jdbc.query(SELECT + " WHERE e.status='em-analise' ORDER BY e.submitted_at,e.id", MAPPER);
    }

    public void insert(Evidence evidence) {
        jdbc.update("INSERT INTO evidence(id,mission_id,participant_id,evidence_text,status,cycle_day,submitted_at,points_snapshot,awarded_points) VALUES(?,?,?,?,?,?,?,?,?)",
            evidence.id(), evidence.missionId(), evidence.participantId(), evidence.text(), evidence.status(),
            evidence.cycleDay(), evidence.submittedAt(), evidence.pointsSnapshot(), evidence.awardedPoints());
    }

    public void resubmit(String id, String text, OffsetDateTime now) {
        jdbc.update("UPDATE evidence SET evidence_text=?,status='em-analise',submitted_at=?,reviewed_at=NULL,awarded_points=0 WHERE id=?", text, now, id);
    }

    public void review(String id, String status, OffsetDateTime now, int points) {
        jdbc.update("UPDATE evidence SET status=?,reviewed_at=?,awarded_points=? WHERE id=?", status, now, points, id);
    }
}
