package br.com.fiap.alpha4.bo;

import br.com.fiap.alpha4.dao.EvidenceDao;
import br.com.fiap.alpha4.dao.ParticipantDao;
import br.com.fiap.alpha4.exception.ApiException;
import br.com.fiap.alpha4.model.Evidence;
import br.com.fiap.alpha4.model.Inputs.EvidenceInput;
import br.com.fiap.alpha4.model.Inputs.Message;
import br.com.fiap.alpha4.model.Inputs.ReviewInput;
import br.com.fiap.alpha4.model.Mission;
import br.com.fiap.alpha4.model.Participant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EvidenceBo {
    private final EvidenceDao evidence;
    private final ParticipantDao participants;
    private final MissionBo missions;
    private final Clock clock;

    public EvidenceBo(EvidenceDao evidence, ParticipantDao participants, MissionBo missions, Clock clock) {
        this.evidence = evidence; this.participants = participants; this.missions = missions; this.clock = clock;
    }

    @Transactional
    public Message submit(String missionId, Participant participant, EvidenceInput input) {
        String text = input.text().trim();
        if (text.length() < 10) throw ApiException.badRequest("Descreva sua participação com pelo menos dez caracteres.");
        participants.lock(participant.id());
        Mission mission = missions.find(missionId, participant);
        LocalDate day = LocalDate.now(clock);
        OffsetDateTime now = OffsetDateTime.now(clock);
        Evidence previous = evidence.findToday(missionId, participant.id(), day).orElse(null);
        if (previous != null) {
            if (!previous.status().equals("rejeitada")) throw ApiException.conflict("Você já enviou uma participação para esta missão hoje.");
            evidence.resubmit(previous.id(), text, now);
        } else {
            evidence.insert(new Evidence(UUID.randomUUID().toString(), missionId, mission.title(), participant.id(),
                text, "em-analise", day, now, null, mission.points(), 0));
        }
        return new Message("Participação recebida. Aguarde a validação da equipe; os pontos ainda não foram creditados.");
    }

    public List<Evidence> own(Participant participant) { return evidence.own(participant.id()); }
    public List<Evidence> pending() { return evidence.pending(); }

    @Transactional
    public Message review(String id, ReviewInput input) {
        Evidence current = evidence.lock(id).orElseThrow(() -> ApiException.notFound("Evidência"));
        if (!current.status().equals("em-analise")) throw ApiException.conflict("Esta evidência já foi analisada.");
        int points = input.status().equals("aprovada") ? current.pointsSnapshot() : 0;
        evidence.review(id, input.status(), OffsetDateTime.now(clock), points);
        return new Message(points > 0 ? "Evidência aprovada e pontos creditados uma única vez." : "Evidência rejeitada. Nenhum ponto foi creditado.");
    }
}
