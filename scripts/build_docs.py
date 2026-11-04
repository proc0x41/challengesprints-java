"""Gera o PDF Java a partir do contrato, diagramas e capturas reais versionados."""
from pathlib import Path
import os
import re
import sys
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

# Opcional para um ambiente sem instalação global: diretório de dependências.
if os.environ.get('ALPHA4_PDF_TOOLS'):
    sys.path.insert(0, os.environ['ALPHA4_PDF_TOOLS'])

from PIL import Image as PILImage
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, A3, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph, Table, TableStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parent.parent
API_VERSION = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
OUTPUT = ROOT / 'output/pdf/Alpha4_Sprint4_Java.pdf'
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
NAVY = colors.HexColor('#111c3d')
GREEN = colors.HexColor('#087f68')
MUTED = colors.HexColor('#475569')
PALE = colors.HexColor('#e5f6ef')
LINE = colors.HexColor('#d9e0e8')

font_candidates = [
    (Path('C:/Windows/Fonts/segoeui.ttf'), Path('C:/Windows/Fonts/segoeuib.ttf')),
    (Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'), Path('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')),
]
FONT, BOLD = 'Helvetica', 'Helvetica-Bold'
for regular, bold in font_candidates:
    if regular.exists() and bold.exists():
        pdfmetrics.registerFont(TTFont('Alpha4', str(regular)))
        pdfmetrics.registerFont(TTFont('Alpha4-Bold', str(bold)))
        pdfmetrics.registerFontFamily('Alpha4', normal='Alpha4', bold='Alpha4-Bold')
        FONT, BOLD = 'Alpha4', 'Alpha4-Bold'
        break

styles = {
    'body': ParagraphStyle('body', fontName=FONT, fontSize=10.5, leading=16, textColor=MUTED, spaceAfter=9),
    'small': ParagraphStyle('small', fontName=FONT, fontSize=8.5, leading=12, textColor=MUTED),
    'table': ParagraphStyle('table', fontName=FONT, fontSize=8.5, leading=12, textColor=NAVY),
    'tablehead': ParagraphStyle('tablehead', fontName=BOLD, fontSize=8.5, leading=12, textColor=NAVY),
    'code': ParagraphStyle('code', fontName='Courier', fontSize=8.5, leading=13, textColor=NAVY),
}

pdf = canvas.Canvas(str(OUTPUT), pagesize=A4, pageCompression=1)
pdf.setTitle('Alpha4 - Soul Up - Documentação Java - Sprint 4')
pdf.setAuthor('Equipe Alpha4 - FIAP 1TDSPW')
pdf.setSubject('Spring Boot, API REST, JDBC, MER, classes e integração real com frontend')
page_count = 0
width, height = A4
margin = 36
y = height - 76


def paragraph(text, x=margin, top=None, available=None, style='body'):
    global y
    if top is None:
        top = y
    if available is None:
        available = width - x - margin
    item = Paragraph(text, styles[style])
    _, h = item.wrap(available, height)
    if top - h < 44:
        raise ValueError(f'Conteúdo ultrapassou a página {page_count}: {text[:70]}')
    item.drawOn(pdf, x, top - h)
    y = top - h - 10
    return h


def title(text, size=23):
    global y
    pdf.setFont(BOLD, size)
    pdf.setFillColor(NAVY)
    pdf.drawString(margin, height - 72, text)
    y = height - 98


def page(text, size=A4):
    global page_count, width, height, y
    if page_count:
        pdf.showPage()
    page_count += 1
    width, height = size
    pdf.setPageSize(size)
    pdf.setFillColor(GREEN)
    pdf.rect(margin, height - 33, 28, 4, fill=1, stroke=0)
    pdf.setFont(FONT, 8)
    pdf.setFillColor(MUTED)
    pdf.drawString(margin + 37, height - 32, 'ALPHA4 / SOUL UP - JAVA - SPRINT 4')
    pdf.setStrokeColor(LINE)
    pdf.line(margin, 34, width - margin, 34)
    pdf.setFont(FONT, 8)
    pdf.drawString(margin, 20, 'FIAP - 1TDSPW | Documentação técnica | 03/11/2026')
    pdf.drawRightString(width - margin, 20, str(page_count))
    title(text)
    pdf.bookmarkPage(f'page-{page_count}')
    pdf.addOutlineEntry(text, f'page-{page_count}', level=0)


def heading(text):
    global y
    y -= 7
    pdf.setFillColor(NAVY)
    pdf.setFont(BOLD, 13)
    pdf.drawString(margin, y - 13, text)
    y -= 26


def table(rows, column_widths, header=True):
    global y
    data = [[Paragraph(escape(str(cell)), styles['tablehead' if header and i == 0 else 'table']) for cell in row] for i, row in enumerate(rows)]
    item = Table(data, colWidths=column_widths, repeatRows=1 if header else 0, hAlign='LEFT')
    item.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('BACKGROUND', (0, 0), (-1, 0), PALE if header else colors.white),
        ('LEFTPADDING', (0, 0), (-1, -1), 7),
        ('RIGHTPADDING', (0, 0), (-1, -1), 7),
        ('TOPPADDING', (0, 0), (-1, -1), 7),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 7),
        ('LINEBELOW', (0, 0), (-1, 0), 0.7, GREEN),
        ('LINEBELOW', (0, 1), (-1, -1), 0.3, LINE),
    ]))
    _, h = item.wrap(width - 2 * margin, height)
    if y - h < 45:
        raise ValueError(f'Tabela ultrapassou a página {page_count}.')
    item.drawOn(pdf, margin, y - h)
    y -= h + 16


def code(text):
    for line in text.splitlines():
        paragraph(escape(line).replace(' ', '&nbsp;'), style='code')


def screenshot(filename, label, explanation):
    global y
    page(label, landscape(A3))
    paragraph(explanation, available=min(width - 2 * margin, 1000))
    image = ROOT / 'docs/screenshots' / filename
    iw, ih = PILImage.open(image).size
    max_height = y - 75
    ratio = min((width - 2 * margin) / iw, max_height / ih)
    dw, dh = iw * ratio, ih * ratio
    pdf.drawImage(str(image), (width - dw) / 2, y - dh, width=dw, height=dh)
    paragraph(f'Fonte: captura real do frontend integrado, 03/11/2026. Arquivo: docs/screenshots/{filename}.', top=68, style='small')


def diagram(path, label, explanation):
    global y
    # Os PNGs 3x são renderizados pelo navegador. Assim, rótulos HTML/CSS
    # do Mermaid não se perdem na conversão SVG -> PDF.
    image_path = path.with_suffix('.png')
    iw, ih = PILImage.open(image_path).size
    page(label, A3 if ih > iw * 1.1 else landscape(A3))
    paragraph(explanation, available=min(width - 2 * margin, 1000))
    ratio = min((width - 2 * margin) / iw, (y - 85) / ih)
    draw_width, draw_height = iw * ratio, ih * ratio
    pdf.drawImage(str(image_path), (width - draw_width) / 2, y - draw_height, width=draw_width, height=draw_height)
    paragraph(f'Fonte versionada: {path.relative_to(ROOT).as_posix()}. Prancha ampliada para preservar a legibilidade.', top=67, style='small')


page('Documentação da API Java')
paragraph('<b>Equipe Alpha4</b><br/>Sistema de gamificação para a Soul Up<br/>Entrega da Sprint 4 - Domain Driven Design Using Java')
heading('Integrantes')
table([
    ['Nome completo', 'RM'],
    ['Vinicius de Oliveira', '561862'],
    ['Robison Custodio Junior', '572866'],
    ['Lucas Fernando Silva', '564848'],
    ['Thiago Shimizu Sodré dos Santos', '572867'],
    ['Arthur Mendes Garcia', '571703'],
], [width - 2 * margin - 80, 80])
paragraph('<b>Turma:</b> 1TDSPW - FIAP, São Paulo<br/><b>Stack:</b> Java 17, Spring Boot 3.5.16, JDBC, H2/PostgreSQL e Maven Wrapper.')
heading('Código e situação da publicação')
paragraph('Repositório Java: <link href="https://github.com/proc0x41/challengesprints-java" color="#087f68">github.com/proc0x41/challengesprints-java</link>.<br/>Frontend: <link href="https://github.com/proc0x41/chellengesprints-alpha4" color="#087f68">github.com/proc0x41/chellengesprints-alpha4</link>.')
paragraph('<b>Deploy em nuvem:</b> ainda pendente. Este documento comprova a execução e a integração locais; não apresenta uma URL pública de API como se estivesse publicada.')
paragraph('As capturas desta documentação são reais e foram produzidas com o frontend consumindo o backend local. As pranchas de imagens e diagramas usam páginas ampliadas para manter textos e relações legíveis.', style='small')

page('Objetivo, escopo e arquitetura')
paragraph('A Alpha4 propõe transformar atitudes sustentáveis em uma experiência recorrente por meio de missões, evidências, pontuação e participação coletiva. A API conecta as telas React ao armazenamento relacional e concentra as regras de negócio.')
heading('Funcionalidades implementadas')
for item in [
    '<b>Missões:</b> listagem, detalhe, criação, edição e exclusão administrativa; estado diário calculado por participante.',
    '<b>Perfis:</b> criação de pseudônimo e sessão, leitura, edição e exclusão do próprio perfil; UF e comunidade opcional.',
    '<b>Evidências:</b> envio textual, consulta própria, fila administrativa e aprovação ou rejeição explícita.',
    '<b>Ranking:</b> pontuação persistida nas abrangências global, estadual, comunidade e amigos, por temporada bimestral.',
    '<b>Acompanhamentos:</b> inclusão e remoção reais de participantes na lista de amigos, sem amizade fictícia.',
]:
    paragraph(item)
heading('Camadas e padrões utilizados')
paragraph('<b>Resource - BO - DAO - banco:</b> controladores definem REST; BOs aplicam validações, autorização e transações; DAOs executam SQL parametrizado. ConnectionFactory centraliza o DataSource via JdbcTemplate, que gerencia conexões em conjunto com as transações Spring.')
paragraph('CorsFilter aceita apenas origens configuradas. Sessões usam tokens aleatórios e somente hashes SHA-256 são persistidos. CRUD de missões e moderação exigem chave administrativa separada. JSON usa tipos estritos, sem converter strings ou números fracionários silenciosamente.')
heading('Limites assumidos de forma explícita')
paragraph('Guerra de Comunidades, recompensas e equivalências científicas de CO2 permanecem na proposta da Sprint 3 e não são funcionalidades entregues por esta API. As três missões iniciais são exemplos educacionais com pontos demonstrativos; não existem perfis fictícios no seed. A entrega de IA é independente.')

contract = (ROOT / 'docs/CONTRATO_API.md').read_text(encoding='utf-8')
endpoint_rows = []
for line in contract.splitlines():
    if re.match(r'^\| (GET|POST|PUT|DELETE) \|', line):
        endpoint_rows.append([part.strip().replace('\\|', '/') for part in re.split(r'(?<!\\)\|', line)[1:-1]])
for number, rows in enumerate([endpoint_rows[:11], endpoint_rows[11:]], 1):
    page(f'Endpoints REST ({number}/2)')
    paragraph('Base local: <b>http://localhost:8080/api</b>. Corpo e respostas em JSON UTF-8. A versão textual completa do contrato está em docs/CONTRATO_API.md.')
    table([['Método', 'URI após /api', 'Acesso', 'OK', 'Falhas']] + rows,
          [53, 215, 100, 45, width - 2 * margin - 413])
    paragraph('<b>Bearer:</b> token da sessão do participante. <b>Administrativa:</b> cabeçalho X-Admin-Key igual a ADMIN_KEY. Chave administrativa ausente na configuração desabilita essas operações com 503. Nunca enviá-la ao bundle frontend.', style='small')
    paragraph('Toda operação também pode retornar 500 em indisponibilidade interna ou do banco. Recursos inexistentes retornam 404; método não permitido, 405; formato não JSON, 415. Erros contêm somente a propriedade message, sem SQL, tokens ou stacktrace.', style='small')

page('Contratos e regras de negócio')
heading('Exemplo de missão retornada')
code('{"id":"uuid", "title":"Separar recicláveis",\n "description":"Orientações da missão",\n "category":"ambiental", "points":20,\n "requiresEvidence":true, "status":"disponivel"}')
paragraph('Categoria: ambiental, social ou governanca. Estado: disponivel, em-analise ou concluida. points é inteiro de 1 a 10.000; requiresEvidence é booleano. POST/PUT recebem os campos de conteúdo, sem id ou status. Tipos incompatíveis são rejeitados com 400.')
heading('Perfil e autenticação')
paragraph('POST /participantes recebe name, uf e community opcional e retorna participant e token. O pseudônimo possui de 2 a 80 caracteres após trim; a comunidade aceita até 80 e é normalizada para string vazia se omitida. A UF é uma sigla brasileira válida. O perfil não exige dados pessoais ou senha e não possui recuperação por e-mail.')
heading('Ciclo de evidência e crédito')
paragraph('POST /missoes/{id}/evidencias recebe text de 10 a 2.000 caracteres. O envio fica em análise e não credita pontos. Há uma evidência por missão, participante e dia civil em America/Sao_Paulo. Uma rejeitada pode ser corrigida; em análise ou aprovada, novo envio no mesmo dia retorna 409.')
paragraph('Os pontos da missão são registrados no envio (points_snapshot). A aprovação administrativa utiliza esse valor e é transacional: requisições simultâneas não duplicam o crédito. Alterar a missão não altera o valor já prometido a evidências anteriores. Missão com histórico não pode ser excluída (409).')
heading('Temporadas e segmentação')
paragraph('O ranking soma apenas evidências aprovadas cujo dia pertence ao bimestre corrente. Empates são ordenados por pseudônimo e ID. Estadual usa a UF do perfil; comunidade compara o nome sem diferenciar maiúsculas e retorna vazio para perfil sem grupo; amigos inclui o próprio perfil e os participantes efetivamente acompanhados.')

screenshot('missoes-desktop.png', 'Tela real: listagem de missões',
    'GET /api/missoes fornece os títulos, orientações, categorias e pontos exibidos. O filtro por categoria é local à interface e não substitui a fonte persistida. Nenhum erro de conexão é apresentado como lista fictícia.')
screenshot('detalhe-desktop.png', 'Tela real: detalhe e envio de evidência',
    'A rota dinâmica /missoes/:id consulta GET /api/missoes/{id}. O formulário envia POST /api/missoes/{id}/evidencias com a sessão Bearer. O backend valida o texto e mantém a participação em análise até a moderação explícita.')
screenshot('ranking-desktop.png', 'Tela real: ranking integrado',
    'GET /api/ranking retorna temporada, posições e pontuação. As posições mostradas são registros reais do ambiente local de demonstração, não resultados ambientais mensurados. Os escopos pessoais usam a sessão do participante.')

page('Tela real: perfil em dispositivo móvel', landscape(A3))
paragraph('Captura completa do perfil responsivo em 390 px, dividida em três faixas com pequena sobreposição para preservar linhas nas transições. Ler da esquerda para a direita. O perfil permite criar/editar a sessão e acompanhar participantes reais; sem chave administrativa no navegador.')
image = ROOT / 'docs/screenshots/perfil-mobile.png'
iw, ih = PILImage.open(image).size
gap = 24
panel_width = (width - 2 * margin - 2 * gap) / 3
segment_height = ih / 3 + 50
ratio = min(panel_width / iw, (y - 95) / segment_height)
draw_width = iw * ratio
segment_printed = segment_height * ratio
for part in range(3):
    start = max(0, part * (ih / 3) - (50 if part else 0))
    left = margin + part * (panel_width + gap) + (panel_width - draw_width) / 2
    bottom = y - segment_printed
    pdf.saveState()
    clip = pdf.beginPath()
    clip.rect(left, bottom, draw_width, segment_printed)
    pdf.clipPath(clip, stroke=0, fill=0)
    pdf.drawImage(str(image), left, y - ih * ratio + start * ratio,
                  width=draw_width, height=ih * ratio)
    pdf.restoreState()
    pdf.setFillColor(MUTED)
    pdf.setFont(FONT, 9)
    pdf.drawString(left, bottom - 17, f'Parte {part + 1}/3')
paragraph('Fonte: docs/screenshots/perfil-mobile.png. Captura real contínua do frontend integrado, 03/11/2026.', top=66, style='small')

diagram(ROOT / 'docs/mer.svg', 'Modelo de Entidade-Relacionamento',
    'Modelo compatível com src/main/resources/schema.sql. IDs são chaves primárias; evidências referenciam missão e participante. O trio missão/participante/dia é único. Acompanhamentos possuem chave composta e impedem autoacompanhamento. O token é persistido somente por hash.')

class_diagrams = [
    ('classes-01-dominio', 'Classes: modelos de domínio', 'Records imutáveis representam os dados do domínio e do ranking. Category restringe categorias. Os identificadores de Evidence referenciam Mission e Participant; Ranking contém suas entradas.'),
    ('classes-02-dtos', 'Classes: contratos de entrada e saída', 'DTOs declarados em Inputs.java separam os dados aceitos pela API dos modelos persistidos. Validações Jakarta e BOs impõem campos, tipos e limites. Session contém o perfil e o token entregue uma única vez na criação.'),
    ('classes-03-regras-missoes', 'Classes: regras de missões e evidências', 'MissionBo concentra o CRUD; EvidenceBo organiza envio diário e revisão em transação. SessionBo autentica participantes e autoriza administração. Caixas marcadas como referência têm seus detalhes nas pranchas da própria camada.'),
    ('classes-04-regras-perfis', 'Classes: perfis e temporadas', 'ParticipantBo normaliza perfis e mantém acompanhamentos; RankingBo delimita abrangência e bimestre a partir do Clock em America/Sao_Paulo. As consultas ficam nos respectivos DAOs.'),
    ('classes-05-dao-missoes', 'Classes: persistência de missões e evidências', 'Os DAOs executam SQL parametrizado e mapeiam registros. EvidenceDao usa bloqueio no processo de revisão; MissionDao calcula o estado diário individual. ConnectionFactory fornece o JdbcTemplate compartilhado.'),
    ('classes-06-dao-perfis', 'Classes: persistência de perfis e ranking', 'ParticipantDao mantém perfis e relações reais. RankingDao filtra apenas escopos permitidos e soma pontos aprovados da temporada. A conexão é centralizada, sem SQL espalhado pelos controladores.'),
    ('classes-07-recursos-missoes', 'Classes: recursos REST de missões', 'MissionResource e EvidenceResource traduzem HTTP para as operações dos BOs. HealthResource verifica a conexão com o banco por SELECT 1. A autorização é aplicada antes das operações administrativas.'),
    ('classes-08-recursos-perfis', 'Classes: recursos de perfil e configuração', 'ParticipantResource e RankingResource expõem operações pessoais e públicas. CorsFilter restringe origens; StrictJsonConfig impede coerções silenciosas de tipos. A fonte canônica completa permanece em docs/classes.mmd.'),
]
for filename, label, explanation in class_diagrams:
    diagram(ROOT / f'docs/diagrams/{filename}.svg', label, explanation)

page('Procedimentos de execução')
heading('Ferramentas e requisitos')
paragraph('JDK 17 ou superior (validado com Microsoft OpenJDK 17.0.18), terminal e rede na primeira instalação. O Maven Wrapper baixa Maven 3.9.11 com verificação da distribuição. Spring Boot 3.5.16 está fixado em pom.xml. IDEs opcionais: IntelliJ, Eclipse ou VS Code com suporte a Java.')
heading('Passo a passo no Windows PowerShell')
code('git clone https://github.com/proc0x41/challengesprints-java\ncd challengesprints-java\njava -version\n.\\mvnw.cmd clean verify\n.\\mvnw.cmd spring-boot:run')
paragraph('Para importar numa IDE, abra pom.xml como projeto Maven, selecione JDK 17 e aguarde a resolução das dependências. Execute SoulUpApplication. Em Linux/macOS, use sh mvnw nos mesmos objetivos Maven.')
heading('Verificar o serviço e consumir pelo frontend')
code('Invoke-RestMethod http://localhost:8080/api/health\nInvoke-RestMethod http://localhost:8080/api/missoes')
paragraph('No frontend, configure VITE_API_URL=http://localhost:8080/api. O backend permite por padrão as origens http://localhost:5173 e http://127.0.0.1:5173. As chamadas usam fetch e respostas tipadas, com estados de carregamento, vazio e erro.')
heading('Executar o artefato empacotado')
code(f'.\\mvnw.cmd package\njava -jar target/soul-up-api-{API_VERSION}.jar')
paragraph('O banco H2 fica em data/soulup.mv.db e persiste entre reinicializações. Os testes usam outro banco, isolado em memória, e não alteram o banco do ambiente de demonstração.')

page('Configuração, verificação e entrega')
table([
    ['Variável de ambiente', 'Finalidade'],
    ['PORT', 'Porta HTTP; padrão 8080.'],
    ['ADMIN_KEY', 'Chave aleatória para CRUD e moderação; vazia resulta em 503. Nunca VITE_*.'],
    ['CORS_ORIGINS', 'Origens exatas permitidas, separadas por vírgula.'],
    ['JDBC_DATABASE_URL', 'URL JDBC H2 local ou PostgreSQL para nuvem.'],
    ['DB_USER / DB_PASSWORD', 'Credenciais do banco, configuradas fora do repositório.'],
    ['SEED_DEMO', 'Habilita exemplos educacionais somente se a tabela estiver vazia.'],
], [135, width - 2 * margin - 135])
paragraph('O arquivo .env.example é uma referência; Spring Boot não o lê automaticamente. Exporte as variáveis no shell ou configure-as no serviço. Para container, use o Dockerfile fornecido. Em nuvem, configure PostgreSQL persistente; H2 sem volume não preserva dados ao substituir o container.')
heading('Evidências de verificação')
paragraph('<b>11 testes de integração passaram</b>, sem falhas, erros ou testes ignorados: CRUD, validações de tipos estritos, limites após trim, hash do token, correção de evidência, snapshot de pontos, concorrência no envio e na aprovação, meia-noite de São Paulo, temporadas e CORS. Relatório reproduzível: .\\mvnw.cmd test.')
paragraph('A revisão independente encontrou coerção indevida de JSON e divergência nos limites antes do trim; ambas foram corrigidas e cobertas por regressões HTTP. A integração real do frontend foi exercitada com persistência e moderação explícita. As capturas deste PDF documentam essas telas locais.')
heading('Conteúdo da entrega e pendências')
paragraph('O ZIP de fontes inclui código Java, pom.xml, Maven Wrapper, configurações, contratos, fontes/renderizações dos diagramas, capturas reais, script desta documentação e este PDF. Exclui .git, target, banco data, variáveis locais e tokens.')
paragraph('<b>Permanece pendente o deploy na nuvem.</b> O link público será registrado no README e nesta documentação somente após publicação e verificação. O frontend exige também sua própria publicação na Vercel. Nenhuma dessas pendências é apresentada como concluída.')

pdf.save()
print(f'PDF criado: {OUTPUT.relative_to(ROOT)} ({page_count} páginas).')
