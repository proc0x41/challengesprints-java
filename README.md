# Alpha4 / Soul Up: API Java

API REST de missões sustentáveis, evidências e ranking, construída do zero para
a Sprint 4. O frontend React consome esta API; a entrega de IA é independente.

## Tecnologias e ferramentas

- Java JDK 17 ou superior; execução validada com Microsoft OpenJDK 17.0.18.
- Spring Boot 3.5.16, Spring Web, Validation e JDBC.
- Maven 3.9.11 pelo Maven Wrapper incluído; nenhuma IDE é obrigatória.
- H2 em arquivo para execução local e driver PostgreSQL para nuvem.
- JUnit 5, AssertJ e TestRestTemplate nos testes reais de integração.
- Dockerfile de múltiplas etapas para servidores que aceitam containers.

Pode abrir/importar `pom.xml` como projeto Maven no IntelliJ, Eclipse ou VS
Code com suporte Java. A execução pelo terminal independe desses editores.

## Como executar

No Windows PowerShell, a partir da pasta deste projeto:

```powershell
java -version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

No Linux/macOS: `sh mvnw clean verify` e `sh mvnw spring-boot:run`.
A primeira execução baixa o Maven e as dependências pela rede.

API local: `http://localhost:8080/api`. Saúde e banco:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/missoes
```

O banco local fica em `data/soulup.mv.db` e persiste entre reinicializações.
Não distribua o banco de uso real nem tokens junto à entrega. As três missões
iniciais são exemplos educacionais; não são registros fictícios de usuários
nem medições científicas de impacto ambiental. `SEED_DEMO=false` desativa o
cadastro inicial. Nenhum ponto é creditado sem moderação explícita.

## Configuração

As variáveis de `.env.example` são uma referência. Spring Boot **não** carrega
esse arquivo automaticamente: exporte as variáveis no shell ou configure-as
no servidor. Não versione segredos nem use `VITE_ADMIN_KEY`.

| Variável | Uso / padrão |
| --- | --- |
| PORT | Porta HTTP; 8080 |
| ADMIN_KEY | Chave longa e aleatória para CRUD e moderação; vazia desabilita administração com 503 |
| CORS_ORIGINS | Origens exatas separadas por vírgula; localhost/127.0.0.1:5173 |
| SEED_DEMO | Cadastrar exemplos se a tabela de missões estiver vazia; true |
| JDBC_DATABASE_URL | JDBC H2 local ou URL `jdbc:postgresql://...` da instância do grupo |
| DB_USER / DB_PASSWORD | Credenciais do banco; local H2 sa/senha vazia |

Administração utiliza `X-Admin-Key`. Operações pessoais usam Bearer, obtido
criando um pseudônimo em `POST /api/participantes`. O banco armazena somente
o hash SHA-256 do token. O perfil é uma sessão de demonstração sem senha:
não há recuperação de conta por e-mail nem autenticação de identidade real.

## Recursos e regras

O [contrato REST completo](docs/CONTRATO_API.md) contém payloads, limites,
autorização, URIs, verbos e códigos de resposta. Principais recursos:

- CRUD completo de missões, com escrita administrativa.
- Criação, leitura, edição e exclusão do próprio perfil.
- Envio de evidência textual, consulta própria e moderação administrativa.
- Listas persistidas de participantes acompanhados.
- Rankings global, estadual, comunidade e amigos, por temporada bimestral.

Missões usam o dia civil de `America/Sao_Paulo`, com reinício à meia-noite,
e não uma janela móvel de 24 horas. Há uma evidência por missão/perfil/dia.
Uma rejeitada pode ser corrigida; pendentes/aprovadas não podem ser duplicadas.
Os pontos ficam congelados no envio, são creditados somente na aprovação e
não se duplicam quando duas requisições tentam aprovar simultaneamente.

O ranking soma evidências aprovadas da temporada bimestral corrente. Não há
perfis fictícios no seed. O ranking de amigos inclui o perfil e as pessoas
que ele acompanha, sem alegar amizade recíproca. Sem comunidade cadastrada,
o ranking de comunidade retorna vazio.

Guerra de Comunidades, vitrine de recompensas e equivalências científicas de
CO₂ são propostas do documento da Sprint 3 e não fazem parte do recorte
implementado nesta API. Não há aprovação automática da veracidade de textos.

## Organização e padrões

```text
src/main/java/br/com/fiap/alpha4/
  model/       Modelos imutáveis, enum e DTOs de entrada
  dao/         SQL parametrizado e mapeamento JDBC
  bo/          Validações, autorização e regras de negócio
  resource/    Controladores REST e status HTTP
  config/      ConnectionFactory, CorsFilter e conteúdo inicial
  exception/   Exceções e respostas padronizadas
src/main/resources/
  schema.sql   Tabelas, relacionamentos e restrições
  application.properties
src/test/      Integração HTTP real com H2 isolado em memória
docs/          Contrato, MER e diagrama de classes
```

`ConnectionFactory` centraliza o DataSource por meio de JdbcTemplate; o Spring
gerencia a abertura/liberação de conexões e as transações dos BOs. Não se
mistura fechamento manual de conexão com transação gerenciada. CORS permite
somente as origens configuradas. Todos os valores externos usados no SQL
são parâmetros; os filtros de ranking seguem uma lista fechada de opções.

Fontes dos diagramas: [MER](docs/mer.mmd) e
[classes](docs/classes.mmd), compatíveis com o esquema e o código entregue.

## Verificação

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/soul-up-api-0.1.1.jar
```

A suíte verifica CRUD, limites, token armazenado por hash, perfis opcionais,
evidências corrigidas, snapshot de pontos, envio e aprovação concorrentes,
reinício à meia-noite de São Paulo, troca de temporada e CORS. Ela usa banco
separado em memória e não altera `data/soulup.mv.db`.

## Deploy e entrega

Imagem: `docker build -t alpha4-soul-up .`. O container escuta `PORT` ou 8080.
Em nuvem, configure PostgreSQL persistente, `ADMIN_KEY` e a origem Vercel
exata em `CORS_ORIGINS`. H2 dentro de um container sem volume perde dados
ao substituir o container, por isso não é a configuração indicada para o
deploy persistente. Não exponha o banco diretamente à internet.

Repositório: [proc0x41/challengesprints-java](https://github.com/proc0x41/challengesprints-java).
A [documentação PDF](output/pdf/Alpha4_Sprint4_Java.pdf) inclui capa, escopo,
endpoints, capturas reais do frontend integrado, MER, classes e execução.

**Pendência externa:** não há URL de deploy em nuvem publicada nesta etapa.
O serviço de hospedagem e o banco remoto ainda precisam ser configurados.

### Reproduzir a documentação e gerar o ZIP

As imagens usadas estão em `docs/screenshots`, copiadas de capturas reais do
frontend integrado em 03/11/2026. Não são telas geradas por IA ou imagens
fictícias. Os diagramas têm fontes Mermaid canônicas e recortes por camada.

```powershell
python -m pip install -r scripts/requirements-docs.txt
python scripts/build_docs.py
python scripts/render_pdf.py
python scripts/package_sources.py
```

Para regenerar os SVGs, é necessário Node/npm e um Chromium disponível:
`scripts/render-diagrams.ps1 -BrowserPath 'caminho/do/chrome.exe'`. O script
usa Mermaid CLI 12.2.0 e deriva os recortes de `docs/classes.mmd`. O PDF usa
Segoe UI quando disponível ou DejaVu Sans em Linux; caso contrário, Helvetica.
As páginas PNG de revisão ficam em `tmp/pdfs/review` e não são versionadas.

O ZIP e seu SHA-256 ficam em `entregas/`. O empacotamento usa uma lista
permitida de fontes/documentação, valida a integridade do ZIP e exclui banco,
tokens, `.git`, `target` e arquivos temporários. O PDF final está incluído.

## Autores e contato

Turma **1TDSPW**, FIAP. Identidades conforme `Alpha4_Sprint3.pdf` fornecido.

| Nome | RM | Contato |
| --- | --- | --- |
| Vinicius de Oliveira | 561862 | 561862@fiap.com.br |
| Robison Custodio Junior | 572866 | 572866@fiap.com.br |
| Lucas Fernando Silva | 564848 | 564848@fiap.com.br |
| Thiago Shimizu Sodré dos Santos | 572867 | 572867@fiap.com.br |
| Arthur Mendes Garcia | 571703 | 571703@fiap.com.br |

O histórico usa Conventional Commits com descrições em português, identidades
locais alternadas e branches GitFlow `main`, `develop`, `feature/*`. Não há
afirmação de autoria manual individual do código gerado com assistência.
