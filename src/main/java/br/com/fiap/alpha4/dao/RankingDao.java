package br.com.fiap.alpha4.dao;

import br.com.fiap.alpha4.config.ConnectionFactory;
import br.com.fiap.alpha4.model.Participant;
import br.com.fiap.alpha4.model.Ranking.Entry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RankingDao {
    private final JdbcTemplate jdbc;
    public RankingDao(ConnectionFactory factory) { jdbc = factory.jdbc(); }

    public List<Entry> list(String scope, Participant participant, LocalDate start, LocalDate end) {
        String condition = "";
        List<Object> parameters = new ArrayList<>(List.of(start, end));
        switch (scope) {
            case "estadual" -> { condition = " WHERE p.uf=?"; parameters.add(participant.uf()); }
            case "comunidade" -> { condition = " WHERE LOWER(p.community)=LOWER(?)"; parameters.add(participant.community()); }
            case "amigos" -> {
                condition = " WHERE (p.id=? OR EXISTS(SELECT 1 FROM friendships f WHERE f.participant_id=? AND f.friend_id=p.id))";
                parameters.add(participant.id()); parameters.add(participant.id());
            }
            default -> { }
        }
        String sql = """
            SELECT p.id,p.name,COALESCE(SUM(e.awarded_points),0) AS total_points
            FROM participants p LEFT JOIN evidence e ON e.participant_id=p.id
                AND e.status='aprovada' AND e.cycle_day>=? AND e.cycle_day<?
            """ + condition + " GROUP BY p.id,p.name ORDER BY total_points DESC,p.name,p.id";
        return jdbc.query(sql, (rs, row) -> new Entry(rs.getString("id"), row + 1, rs.getString("name"), rs.getLong("total_points")), parameters.toArray());
    }
}
