"""Divide o diagrama canônico por responsabilidade, sem alterar nomes/métodos."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
source = (ROOT / 'docs/classes.mmd').read_text(encoding='utf-8')
blocks = {}
lines = source.splitlines()
index = 0
while index < len(lines):
    match = re.match(r'    class (\w+)\s*(\{)?', lines[index])
    if match:
        start = index
        if match.group(2):
            while lines[index].strip() != '}':
                index += 1
        blocks[match.group(1)] = '\n'.join(lines[start:index + 1])
    index += 1
relations = [line for line in lines if re.match(r'    \w+ [.*<>|\-o]+ \w+', line)]

# Classes de outras camadas aparecem apenas como referências; os detalhes
# completos de cada uma estão no recorte da sua própria camada.
groups = [
    ('classes-01-dominio', ['Mission', 'Participant', 'Evidence', 'Category', 'Ranking', 'Entry'], []),
    ('classes-02-dtos', ['MissionInput', 'ParticipantInput', 'EvidenceInput', 'ReviewInput', 'Session', 'Message'], ['Participant']),
    ('classes-03-regras-missoes', ['MissionBo', 'EvidenceBo', 'SessionBo'], ['MissionDao', 'EvidenceDao', 'ParticipantDao']),
    ('classes-04-regras-perfis', ['ParticipantBo', 'RankingBo', 'SessionBo'], ['ParticipantDao', 'RankingDao']),
    ('classes-05-dao-missoes', ['MissionDao', 'EvidenceDao', 'ConnectionFactory'], []),
    ('classes-06-dao-perfis', ['ParticipantDao', 'RankingDao', 'ConnectionFactory'], []),
    ('classes-07-recursos-missoes', ['MissionResource', 'EvidenceResource', 'HealthResource'], ['MissionBo', 'EvidenceBo', 'SessionBo', 'ConnectionFactory']),
    ('classes-08-recursos-perfis', ['ParticipantResource', 'RankingResource', 'CorsFilter', 'StrictJsonConfig'], ['ParticipantBo', 'RankingBo', 'SessionBo']),
]
target = ROOT / 'docs/diagrams'
target.mkdir(exist_ok=True)
for filename, full_names, reference_names in groups:
    selected = set(full_names + reference_names)
    output = ['classDiagram', '    direction TB']
    output.extend(blocks[name] for name in full_names)
    output.extend(f'    class {name} {{\n        <<referencia>>\n    }}' for name in reference_names)
    for line in relations:
        match = re.match(r'    (\w+) [.*<>|\-o]+ (\w+)', line)
        if match and {match.group(1), match.group(2)} <= selected:
            output.append(line)
    (target / f'{filename}.mmd').write_text('\n'.join(output) + '\n', encoding='utf-8')
print(f'{len(groups)} diagramas derivados de docs/classes.mmd.')
