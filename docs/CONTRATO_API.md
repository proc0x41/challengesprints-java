# Contrato REST Alpha4 / Soul Up

Base local: `http://localhost:8080/api`. JSON UTF-8. Não há integração com a API
de IA: as entregas são independentes, conforme orientação do grupo.

## Sessão de participante

`POST /participantes`, sem autenticação, recebe:

```json
{"name":"Pseudônimo de demonstração","uf":"SP","community":"Alpha4"}
```

Retorna **201**:

```json
{"participant":{"id":"uuid","name":"Pseudônimo de demonstração","uf":"SP","community":"Alpha4"},"token":"token-aleatorio"}
```

Todos os campos de `participant` são strings. Pseudônimo e comunidade têm de
3 a 60 caracteres após retirar espaços nas extremidades; UF precisa ser uma
das 27 siglas brasileiras. Não envie nomes reais ou outros dados pessoais.

Use `Authorization: Bearer <token>` nas operações pessoais. O banco guarda
somente SHA-256 do token. A chave administrativa não é este token.
Sem senha/recuperação nesta entrega: guarde seu token se precisar retomar o
mesmo perfil. O frontend mantém sua sessão temporariamente no navegador.

## Recursos e status

| Método | URI | Autorização | Sucesso | Falhas previstas |
| --- | --- | --- | --- | --- |
| GET | /health | Pública | 200 | 500 |
| POST | /participantes | Pública | 201 | 400 |
| GET | /participantes/me | Bearer | 200 | 401 |
| PUT | /participantes/me | Bearer | 200 | 400, 401 |
| DELETE | /participantes/me | Bearer | 204 | 401 |
| GET | /missoes | Bearer opcional | 200 | 401 |
| GET | /missoes/{id} | Bearer opcional | 200 | 401, 404 |
| POST | /missoes | Administrativa | 201 | 400, 403, 503 |
| PUT | /missoes/{id} | Administrativa | 200 | 400, 403, 404, 503 |
| DELETE | /missoes/{id} | Administrativa | 204 | 403, 404, 409, 503 |
| POST | /missoes/{id}/evidencias | Bearer | 201 | 400, 401, 404, 409 |
| GET | /evidencias/me | Bearer | 200 | 401 |
| GET | /admin/evidencias | Administrativa | 200 | 403, 503 |
| PUT | /admin/evidencias/{id} | Administrativa | 200 | 400, 403, 404, 409, 503 |
| GET | /ranking?scope=global | Bearer opcional | 200 | 400, 401 |
| GET | /ranking?scope=estadual\|comunidade\|amigos | Bearer | 200 | 400, 401 |
| GET | /amigos | Bearer | 200 | 401 |
| POST | /amigos/{id} | Bearer | 201 | 400, 401, 404, 409 |
| DELETE | /amigos/{id} | Bearer | 204 | 401, 404 |

Administração usa `X-Admin-Key` igual à variável de ambiente `ADMIN_KEY`. Se ela
não for configurada, operações administrativas retornam 503. Nunca colocar
essa chave no bundle frontend ou em variável `VITE_*`.

Erros retornam `{"message":"mensagem em português"}`. 500 é genérico e não
expõe SQL, tokens nem stacktrace. Requisições JSON com campos desconhecidos
ou tipos inválidos são rejeitadas com 400. Toda operação pode retornar 500
em falha interna/indisponibilidade do banco.

## Missões

GET /missoes retorna um array. GET /missoes/{id} retorna um objeto:

```json
{"id":"uuid","title":"Separar recicláveis","description":"Orientações da missão","category":"ambiental","points":20,"requiresEvidence":true,"status":"disponivel"}
```

POST e PUT recebem o mesmo formato, sem `id` nem `status`. `title`: 1–120
caracteres não brancos; `description`: 1–3.000 não brancos; `points`: inteiro
de 1 a 10.000; `requiresEvidence`: boolean obrigatório; `category`:
`ambiental`, `social` ou `governanca`.

`status` é `disponivel`, `em-analise` ou `concluida`. Sem sessão todas são
disponíveis; com sessão o estado é calculado usando a evidência do participante
no dia civil corrente em America/Sao_Paulo, não uma janela móvel de 24 horas.
Uma missão com histórico não pode ser removida: o banco preserva as evidências
e devolve 409. Edição não altera os pontos já prometidos em evidências enviadas.

## Evidências e pontuação

POST /missoes/{id}/evidencias recebe `{"text":"Descrição da ação realizada"}`,
de 10 a 2.000 caracteres após retirar espaços nas extremidades. Retorna 201
com `{"message":"Participação recebida..."}`. O envio **não credita pontos**.

GET /evidencias/me retorna evidências do participante. GET /admin/evidencias
lista apenas pendentes. PUT /admin/evidencias/{id} recebe
`{"status":"aprovada"}` ou `{"status":"rejeitada"}`.

Existe uma evidência por missão/participante/dia. Em análise ou aprovada,
novo envio no mesmo dia retorna 409. Uma rejeitada pode ser reenviada com
texto corrigido. Aprovação usa o valor de pontos registrado no envio e é
protegida por transação/bloqueio: repetir a análise retorna 409, sem duplicar
pontos. Não existe validação automática da veracidade de uma descrição.

## Ranking e amigos

GET /ranking retorna `{"scope":"global","season":"2026 · 11/12","entries":[]}`.
Cada entrada: `{"id":"uuid","position":1,"name":"Pseudônimo","points":20}`.
`points` soma somente evidências aprovadas cujo dia de participação pertence
à temporada bimestral corrente, em America/Sao_Paulo. Empates são ordenados
por pseudônimo e ID; a posição é ordinal. Perfis reais recém-criados podem
aparecer com zero pontos; não há perfis fictícios no banco inicial.

Estadual usa a UF do próprio perfil; comunidade compara o nome da comunidade
sem distinguir maiúsculas; amigos inclui o próprio participante e aqueles
que ele acompanha via POST /amigos/{id}. É uma lista unilateral de
acompanhamento, não uma alegação de amizade mútua. O ID precisa ser
compartilhado pelo participante; o sistema não publica uma busca de perfis.

## Limites do recorte

Há missões, evidências, perfis, amigos e ranking. Guerra de Comunidades,
recompensas e cálculo científico de CO₂ permanecem propostas do PDF, sem
implementação ou números alegados nesta API. As três missões iniciais são
conteúdo educacional de demonstração e sua pontuação é ilustrativa.
