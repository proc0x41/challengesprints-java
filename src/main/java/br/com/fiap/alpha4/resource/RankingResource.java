package br.com.fiap.alpha4.resource;

import br.com.fiap.alpha4.bo.RankingBo;
import br.com.fiap.alpha4.bo.SessionBo;
import br.com.fiap.alpha4.model.Ranking;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ranking")
public class RankingResource {
    private final RankingBo rankings;
    private final SessionBo sessions;
    public RankingResource(RankingBo rankings, SessionBo sessions) { this.rankings = rankings; this.sessions = sessions; }

    @GetMapping
    public Ranking get(@RequestParam(defaultValue="global") String scope,
                       @RequestHeader(value="Authorization", required=false) String authorization) {
        return rankings.get(scope, sessions.optionalParticipant(authorization));
    }
}
