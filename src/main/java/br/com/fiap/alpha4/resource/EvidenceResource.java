package br.com.fiap.alpha4.resource;

import br.com.fiap.alpha4.bo.EvidenceBo;
import br.com.fiap.alpha4.bo.SessionBo;
import br.com.fiap.alpha4.model.Evidence;
import br.com.fiap.alpha4.model.Inputs.EvidenceInput;
import br.com.fiap.alpha4.model.Inputs.Message;
import br.com.fiap.alpha4.model.Inputs.ReviewInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EvidenceResource {
    private final EvidenceBo evidence;
    private final SessionBo sessions;

    public EvidenceResource(EvidenceBo evidence, SessionBo sessions) { this.evidence = evidence; this.sessions = sessions; }

    @PostMapping("/missoes/{id}/evidencias") @ResponseStatus(HttpStatus.CREATED)
    public Message submit(@PathVariable String id, @RequestHeader(value="Authorization", required=false) String authorization,
                          @Valid @RequestBody EvidenceInput input) {
        return evidence.submit(id, sessions.requireParticipant(authorization), input);
    }

    @GetMapping("/evidencias/me")
    public List<Evidence> own(@RequestHeader(value="Authorization", required=false) String authorization) { return evidence.own(sessions.requireParticipant(authorization)); }

    @GetMapping("/admin/evidencias")
    public List<Evidence> pending(@RequestHeader(value="X-Admin-Key", required=false) String adminKey) {
        sessions.requireAdmin(adminKey);
        return evidence.pending();
    }

    @PutMapping("/admin/evidencias/{id}")
    public Message review(@PathVariable String id, @RequestHeader(value="X-Admin-Key", required=false) String adminKey,
                          @Valid @RequestBody ReviewInput input) {
        sessions.requireAdmin(adminKey);
        return evidence.review(id, input);
    }
}
