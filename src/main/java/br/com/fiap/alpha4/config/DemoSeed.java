package br.com.fiap.alpha4.config;

import br.com.fiap.alpha4.bo.MissionBo;
import br.com.fiap.alpha4.dao.MissionDao;
import br.com.fiap.alpha4.model.Category;
import br.com.fiap.alpha4.model.Inputs.MissionInput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Conteúdo educacional inicial. Os pontos são demonstrativos, não medidas científicas. */
@Component
public class DemoSeed implements ApplicationRunner {
    private final MissionDao dao;
    private final MissionBo missions;
    private final boolean enabled;

    public DemoSeed(MissionDao dao, MissionBo missions, @Value("${app.seed-demo}") boolean enabled) {
        this.dao = dao; this.missions = missions; this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!enabled || dao.count() > 0) return;
        missions.create(new MissionInput("Separe os recicláveis da sua rotina", "Consulte as orientações de coleta do seu município. Separe materiais limpos e secos aceitos localmente e descreva como organizou o descarte. Missão educacional de demonstração: não informa redução científica de CO₂.", Category.AMBIENTAL, 20, true));
        missions.create(new MissionInput("Compartilhe uma orientação de descarte", "Converse com uma pessoa sobre a coleta e o descarte corretos na sua região. Descreva a orientação compartilhada sem identificar a outra pessoa. Pontuação demonstrativa definida para este protótipo.", Category.SOCIAL, 30, true));
        missions.create(new MissionInput("Confira a informação antes de compartilhar", "Consulte uma fonte pública oficial sobre saneamento ou resíduos do município. Descreva a informação e a fonte consultada. Não envie dados pessoais. Pontos demonstrativos, sem equivalência ambiental calculada.", Category.GOVERNANCA, 25, true));
    }
}
