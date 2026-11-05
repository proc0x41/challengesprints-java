"""Verifica a publicacao sem criar/alterar dados ou enviar credenciais."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path
import argparse
import json
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def check(name, url, origin=None, method='GET'):
    headers = {'Accept': 'application/json'}
    if origin:
        headers['Origin'] = origin
    if method == 'OPTIONS':
        headers['Access-Control-Request-Method'] = 'POST'
        headers['Access-Control-Request-Headers'] = 'Content-Type,Authorization'
    record = {'name': name, 'url': url, 'method': method, 'request_headers': headers}
    try:
        request = urllib.request.Request(url, headers=headers, method=method)
        with urllib.request.urlopen(request, timeout=45) as response:
            record['status'] = response.status
            record['response_headers'] = dict(response.headers)
            raw = response.read(20000).decode('utf-8', 'replace')
            try:
                record['body'] = json.loads(raw) if raw else None
            except ValueError:
                record['body'] = raw
    except urllib.error.HTTPError as error:
        record['status'] = error.code
        record['response_headers'] = dict(error.headers)
        record['body'] = error.read(2000).decode('utf-8', 'replace')
    except Exception as error:
        record['error'] = str(error)
    return record


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api-url', default='https://challengesprints-java.onrender.com/api')
    parser.add_argument('--origin', default='https://chellengesprints-alpha4.vercel.app')
    args = parser.parse_args()
    base = args.api_url.rstrip('/')
    jobs = [('health', base + '/health', None, 'GET'),
            ('missions', base + '/missoes', None, 'GET'),
            ('cors_get', base + '/missoes', args.origin, 'GET'),
            ('cors_preflight', base + '/participantes', args.origin, 'OPTIONS')]
    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(lambda job: check(*job), jobs))
    report = {'checked_at': datetime.now().astimezone().isoformat(),
              'api_url': base, 'origin': args.origin, 'checks': results,
              'scope': 'HTTP GET/OPTIONS; nao verifica o provedor do banco nem persistencia apos redeploy'}
    output = ROOT / 'docs/evidencias/deploy.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    for result in results:
        print(result['name'], result.get('status', result.get('error')))
    print('Registro:', output)
