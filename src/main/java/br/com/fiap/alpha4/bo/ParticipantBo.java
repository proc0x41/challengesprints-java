package br.com.fiap.alpha4.bo;

import br.com.fiap.alpha4.dao.ParticipantDao;
import br.com.fiap.alpha4.exception.ApiException;
import br.com.fiap.alpha4.model.Inputs.ParticipantInput;
import br.com.fiap.alpha4.model.Inputs.Session;
import br.com.fiap.alpha4.model.Participant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ParticipantBo {
    private static final Set<String> UFS = Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO");
    private final ParticipantDao participants;
    private final SessionBo sessions;

    public ParticipantBo(ParticipantDao participants, SessionBo sessions) {
        this.participants = participants;
        this.sessions = sessions;
    }

    public Session create(ParticipantInput input) {
        String token = sessions.newToken();
        Participant participant = normalize(UUID.randomUUID().toString(), input);
        participants.insert(participant, sessions.hash(token));
        return new Session(participant, token);
    }

    public Participant update(Participant current, ParticipantInput input) {
        return participants.update(normalize(current.id(), input));
    }

    @Transactional
    public void delete(Participant current) { participants.delete(current.id()); }

    public List<Participant> friends(Participant current) { return participants.friends(current.id()); }

    @Transactional
    public void addFriend(Participant current, String friendId) {
        if (current.id().equals(friendId)) throw ApiException.badRequest("Você não pode acompanhar a si mesmo.");
        participants.lock(current.id());
        participants.find(friendId).orElseThrow(() -> ApiException.notFound("Participante"));
        if (participants.isFriend(current.id(), friendId)) throw ApiException.conflict("Este participante já está na sua lista.");
        participants.addFriend(current.id(), friendId);
    }

    public void deleteFriend(Participant current, String friendId) {
        if (participants.deleteFriend(current.id(), friendId) == 0) throw ApiException.notFound("Acompanhamento");
    }

    private Participant normalize(String id, ParticipantInput input) {
        if (!UFS.contains(input.uf())) throw ApiException.badRequest("Selecione uma UF brasileira válida.");
        String name = input.name().trim();
        String community = input.community() == null ? "" : input.community().trim();
        if (name.length() < 2) throw ApiException.badRequest("O pseudônimo precisa ter pelo menos dois caracteres.");
        return new Participant(id, name, input.uf(), community);
    }
}
