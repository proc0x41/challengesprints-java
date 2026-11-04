package br.com.fiap.alpha4.dao;

import br.com.fiap.alpha4.config.ConnectionFactory;
import br.com.fiap.alpha4.model.Participant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ParticipantDao {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Participant> MAPPER = (rs, row) -> new Participant(
        rs.getString("id"), rs.getString("name"), rs.getString("uf"), rs.getString("community"));

    public ParticipantDao(ConnectionFactory factory) { jdbc = factory.jdbc(); }

    public Participant insert(Participant participant, String tokenHash) {
        jdbc.update("INSERT INTO participants(id,name,uf,community,token_hash) VALUES(?,?,?,?,?)",
            participant.id(), participant.name(), participant.uf(), participant.community(), tokenHash);
        return participant;
    }

    public Optional<Participant> find(String id) {
        return jdbc.query("SELECT id,name,uf,community FROM participants WHERE id=?", MAPPER, id).stream().findFirst();
    }

    public Optional<Participant> findByToken(String tokenHash) {
        return jdbc.query("SELECT id,name,uf,community FROM participants WHERE token_hash=?", MAPPER, tokenHash).stream().findFirst();
    }

    public Participant update(Participant participant) {
        jdbc.update("UPDATE participants SET name=?,uf=?,community=? WHERE id=?",
            participant.name(), participant.uf(), participant.community(), participant.id());
        return participant;
    }

    public void delete(String id) { jdbc.update("DELETE FROM participants WHERE id=?", id); }

    public void lock(String id) { jdbc.queryForObject("SELECT id FROM participants WHERE id=? FOR UPDATE", String.class, id); }

    public List<Participant> friends(String participantId) {
        return jdbc.query("SELECT p.id,p.name,p.uf,p.community FROM participants p JOIN friendships f ON f.friend_id=p.id WHERE f.participant_id=? ORDER BY p.name,p.id", MAPPER, participantId);
    }

    public boolean isFriend(String participantId, String friendId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM friendships WHERE participant_id=? AND friend_id=?", Integer.class, participantId, friendId) > 0;
    }

    public void addFriend(String participantId, String friendId) {
        jdbc.update("INSERT INTO friendships(participant_id,friend_id) VALUES(?,?)", participantId, friendId);
    }

    public int deleteFriend(String participantId, String friendId) {
        return jdbc.update("DELETE FROM friendships WHERE participant_id=? AND friend_id=?", participantId, friendId);
    }
}
