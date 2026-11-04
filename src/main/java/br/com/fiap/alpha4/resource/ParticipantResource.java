package br.com.fiap.alpha4.resource;

import br.com.fiap.alpha4.bo.ParticipantBo;
import br.com.fiap.alpha4.bo.SessionBo;
import br.com.fiap.alpha4.model.Inputs.ParticipantInput;
import br.com.fiap.alpha4.model.Inputs.Session;
import br.com.fiap.alpha4.model.Participant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ParticipantResource {
    private final ParticipantBo participants;
    private final SessionBo sessions;

    public ParticipantResource(ParticipantBo participants, SessionBo sessions) {
        this.participants = participants; this.sessions = sessions;
    }

    @PostMapping("/participantes") @ResponseStatus(HttpStatus.CREATED)
    public Session create(@Valid @RequestBody ParticipantInput input) { return participants.create(input); }

    @GetMapping("/participantes/me")
    public Participant me(@RequestHeader(value="Authorization", required=false) String authorization) { return sessions.requireParticipant(authorization); }

    @PutMapping("/participantes/me")
    public Participant update(@RequestHeader(value="Authorization", required=false) String authorization,
                              @Valid @RequestBody ParticipantInput input) {
        return participants.update(sessions.requireParticipant(authorization), input);
    }

    @DeleteMapping("/participantes/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value="Authorization", required=false) String authorization) { participants.delete(sessions.requireParticipant(authorization)); }

    @GetMapping("/amigos")
    public List<Participant> friends(@RequestHeader(value="Authorization", required=false) String authorization) { return participants.friends(sessions.requireParticipant(authorization)); }

    @PostMapping("/amigos/{id}") @ResponseStatus(HttpStatus.CREATED)
    public void addFriend(@PathVariable String id, @RequestHeader(value="Authorization", required=false) String authorization) { participants.addFriend(sessions.requireParticipant(authorization), id); }

    @DeleteMapping("/amigos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@PathVariable String id, @RequestHeader(value="Authorization", required=false) String authorization) { participants.deleteFriend(sessions.requireParticipant(authorization), id); }
}
