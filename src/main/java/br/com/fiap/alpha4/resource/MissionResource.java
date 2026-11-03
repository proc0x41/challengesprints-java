package br.com.fiap.alpha4.resource;

import br.com.fiap.alpha4.bo.MissionBo;
import br.com.fiap.alpha4.bo.SessionBo;
import br.com.fiap.alpha4.model.Inputs.MissionInput;
import br.com.fiap.alpha4.model.Mission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/missoes")
public class MissionResource {
    private final MissionBo missions;
    private final SessionBo sessions;

    public MissionResource(MissionBo missions, SessionBo sessions) { this.missions = missions; this.sessions = sessions; }

    @GetMapping
    public List<Mission> list(@RequestHeader(value="Authorization", required=false) String authorization) { return missions.list(sessions.optionalParticipant(authorization)); }

    @GetMapping("/{id}")
    public Mission find(@PathVariable String id, @RequestHeader(value="Authorization", required=false) String authorization) { return missions.find(id, sessions.optionalParticipant(authorization)); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Mission create(@RequestHeader(value="X-Admin-Key", required=false) String adminKey, @Valid @RequestBody MissionInput input) {
        sessions.requireAdmin(adminKey);
        return missions.create(input);
    }

    @PutMapping("/{id}")
    public Mission update(@PathVariable String id, @RequestHeader(value="X-Admin-Key", required=false) String adminKey, @Valid @RequestBody MissionInput input) {
        sessions.requireAdmin(adminKey);
        return missions.update(id, input);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, @RequestHeader(value="X-Admin-Key", required=false) String adminKey) {
        sessions.requireAdmin(adminKey);
        missions.delete(id);
    }
}
