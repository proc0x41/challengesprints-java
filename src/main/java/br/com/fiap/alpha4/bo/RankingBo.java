package br.com.fiap.alpha4.bo;

import br.com.fiap.alpha4.dao.RankingDao;
import br.com.fiap.alpha4.exception.ApiException;
import br.com.fiap.alpha4.model.Participant;
import br.com.fiap.alpha4.model.Ranking;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.List;

@Service
public class RankingBo {
    private final RankingDao rankings;
    private final Clock clock;
    private static final Set<String> SCOPES = Set.of("global", "estadual", "comunidade", "amigos");

    public RankingBo(RankingDao rankings, Clock clock) { this.rankings = rankings; this.clock = clock; }

    public Ranking get(String scope, Participant participant) {
        if (!SCOPES.contains(scope)) throw ApiException.badRequest("Abrangência de ranking inválida.");
        if (!scope.equals("global") && participant == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Crie uma sessão para consultar este ranking.");
        LocalDate today = LocalDate.now(clock);
        int firstMonth = ((today.getMonthValue() - 1) / 2) * 2 + 1;
        LocalDate start = LocalDate.of(today.getYear(), firstMonth, 1);
        String season = "%d · %02d/%02d".formatted(today.getYear(), firstMonth, firstMonth + 1);
        if (scope.equals("comunidade") && participant.community().isBlank()) return new Ranking(scope, season, List.of());
        return new Ranking(scope, season, rankings.list(scope, participant, start, start.plusMonths(2)));
    }
}
