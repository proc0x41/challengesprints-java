package br.com.fiap.alpha4.bo;

import br.com.fiap.alpha4.dao.MissionDao;
import br.com.fiap.alpha4.exception.ApiException;
import br.com.fiap.alpha4.model.Inputs.MissionInput;
import br.com.fiap.alpha4.model.Mission;
import br.com.fiap.alpha4.model.Participant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MissionBo {
    private final MissionDao missions;
    private final Clock clock;

    public MissionBo(MissionDao missions, Clock clock) { this.missions = missions; this.clock = clock; }

    public List<Mission> list(Participant participant) {
        return missions.list(participant == null ? "" : participant.id(), LocalDate.now(clock));
    }

    public Mission find(String id, Participant participant) {
        return missions.find(id, participant == null ? "" : participant.id(), LocalDate.now(clock))
            .orElseThrow(() -> ApiException.notFound("Missão"));
    }

    public Mission create(MissionInput input) {
        Mission mission = map(UUID.randomUUID().toString(), input);
        missions.insert(mission);
        return mission;
    }

    @Transactional
    public Mission update(String id, MissionInput input) {
        find(id, null);
        Mission mission = map(id, input);
        missions.update(mission);
        return mission;
    }

    @Transactional
    public void delete(String id) {
        find(id, null);
        missions.delete(id);
    }

    private Mission map(String id, MissionInput input) {
        return new Mission(id, input.title().trim(), input.description().trim(), input.category(), input.points(), input.requiresEvidence(), "disponivel");
    }
}
