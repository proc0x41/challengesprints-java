"""Teste opt-in em PostgreSQL Docker isolado; nao usa dados ou banco remoto.

Requer Docker, JDK e JAR gerado por mvnw clean verify. Cria um container com
nome exclusivo desta verificacao e o remove ao final. Recusa nome existente.
"""
from datetime import datetime
from pathlib import Path
import json
import os
import secrets
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
TMP = ROOT / 'tmp/postgres-check'
TMP.mkdir(parents=True, exist_ok=True)
NAME = 'alpha4-java-postgres-check'
IMAGE = 'postgres@sha256:266d3c257594e195b309ae9d1805e54c788f1d7b375f56bf6c367aac6c917e7b'
BASE = 'http://127.0.0.1:18082/api'
VERSION = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
JAR = ROOT / f'target/soul-up-api-{VERSION}.jar'
assert JAR.is_file(), 'Execute mvnw clean verify primeiro.'
checks = []
process = None
log = None
created = False


def command(*args, check=True):
    return subprocess.run(args, capture_output=True, text=True, check=check, timeout=90)


def request(method, path, body=None, token=None, admin=None):
    headers = {'Content-Type': 'application/json', 'Origin': 'https://chellengesprints-alpha4.vercel.app'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if admin:
        headers['X-Admin-Key'] = admin
    data = json.dumps(body).encode('utf-8') if body is not None else None
    with urllib.request.urlopen(urllib.request.Request(BASE + path, data=data, headers=headers, method=method), timeout=10) as r:
        raw = r.read()
        return r.status, json.loads(raw) if raw else None


def start():
    global process, log
    env = os.environ.copy()
    env.update(SPRING_PROFILES_ACTIVE='render', PORT='18082', DB_HOST='127.0.0.1',
               DB_PORT='55432', DB_NAME='soulup', DB_USER='alpha4', DB_PASSWORD=password,
               DB_SSL_MODE='disable', ADMIN_KEY=admin,
               CORS_ORIGINS='https://chellengesprints-alpha4.vercel.app')
    log = (TMP / 'application.log').open('ab')
    process = subprocess.Popen(['java', '-jar', str(JAR)], cwd=ROOT, env=env,
                               stdout=log, stderr=subprocess.STDOUT,
                               creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    for _ in range(50):
        if process.poll() is not None:
            raise RuntimeError('A API de teste encerrou; veja tmp/postgres-check/application.log.')
        try:
            if request('GET', '/health')[0] == 200:
                return
        except Exception:
            time.sleep(1)
    raise RuntimeError('A API de teste nao ficou pronta.')


def stop():
    global process, log
    if process and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
    if log:
        log.close()
    process = None
    log = None


if command('docker', 'container', 'ls', '-a', '--filter', 'name=^/' + NAME + '$', '--format', '{{.Names}}').stdout.strip():
    raise RuntimeError('O container de teste ja existe; nenhum recurso foi substituido.')
password = secrets.token_hex(24)
admin = secrets.token_hex(32)
envfile = TMP / 'postgres.env'
envfile.write_text('POSTGRES_DB=soulup\nPOSTGRES_USER=alpha4\nPOSTGRES_PASSWORD=' + password + '\n', encoding='utf-8')
try:
    command('docker', 'run', '-d', '--name', NAME, '--publish', '127.0.0.1:55432:5432', '--env-file', str(envfile), IMAGE)
    created = True
    for _ in range(40):
        if command('docker', 'exec', NAME, 'pg_isready', '-U', 'alpha4', '-d', 'soulup', check=False).returncode == 0:
            break
        time.sleep(1)
    pg_version = command('docker', 'exec', NAME, 'psql', '-U', 'alpha4', '-d', 'soulup', '-Atc', 'SHOW server_version').stdout.strip()
    start()
    checks.append('Perfil render conecta ao PostgreSQL real e inicializa o schema')
    status, session = request('POST', '/participantes', {'name': 'Teste PostgreSQL isolado', 'uf': 'SP', 'community': 'Verificacao local'})
    assert status == 201
    token, participant_id = session['token'], session['participant']['id']
    assert request('PUT', '/participantes/me', {'name': 'Teste PostgreSQL atualizado', 'uf': 'RJ', 'community': 'Verificacao local'}, token)[0] == 200
    checks.append('Criacao, leitura e atualizacao de perfil por HTTP')
    missions = request('GET', '/missoes')[1]
    mission = missions[0]
    assert request('POST', '/missoes/' + mission['id'] + '/evidencias', {'text': 'Verificacao isolada do envio de evidencia no PostgreSQL.'}, token)[0] == 201
    evidence = next(e for e in request('GET', '/admin/evidencias', admin=admin)[1] if e['participantId'] == participant_id)
    assert request('PUT', '/admin/evidencias/' + evidence['id'], {'status': 'aprovada'}, admin=admin)[0] == 200
    ranking = request('GET', '/ranking')[1]
    assert next(e for e in ranking['entries'] if e['id'] == participant_id)['points'] == mission['points']
    checks.append('Evidencia, aprovacao transacional e credito no ranking')
    stop()
    start()
    assert request('GET', '/participantes/me', token=token)[1]['name'] == 'Teste PostgreSQL atualizado'
    assert next(e for e in request('GET', '/ranking')[1]['entries'] if e['id'] == participant_id)['points'] == mission['points']
    checks.append('Perfil, token e pontos preservados apos reiniciar a API')
    assert request('DELETE', '/participantes/me', token=token)[0] == 204
    checks.append('Exclusao de perfil e relacionamentos sem violacao de FK')
    report = {'checked_at': datetime.now().astimezone().isoformat(), 'success': True,
              'scope': 'PostgreSQL Docker local isolado; nao e o banco da conta Render',
              'api_version': VERSION, 'postgres_version': pg_version, 'image': IMAGE,
              'profile': 'render', 'tls': 'disable somente no teste local; require no Blueprint',
              'checks': checks}
    out = ROOT / 'docs/evidencias/postgresql-local.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('PASS PostgreSQL', pg_version, '-', len(checks), 'verificacoes; relatorio:', out)
finally:
    stop()
    envfile.unlink(missing_ok=True)
    if created:
        command('docker', 'rm', '-f', NAME, check=False)
