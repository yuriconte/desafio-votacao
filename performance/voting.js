import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const profile = __ENV.PERF_PROFILE || 'smoke';
const runId = __ENV.PERF_RUN_ID || 'manual-smoke';
const rate = positiveInteger(__ENV.PERF_RATE, 100);
const duration = __ENV.PERF_DURATION || '2m';
const totalVotes = positiveInteger(__ENV.PERF_TOTAL_VOTES, 200000);
const vus = positiveInteger(__ENV.PERF_VUS, 100);
const preAllocatedVUs = positiveInteger(__ENV.PERF_PREALLOCATED_VUS, 100);

const votesCreated = new Counter('votes_created');
const votesFailed = new Counter('votes_failed');

const profiles = {
  smoke: {
    executor: 'shared-iterations',
    vus: 1,
    iterations: 10,
    maxDuration: '30s',
  },
  load: {
    executor: 'constant-arrival-rate',
    rate,
    timeUnit: '1s',
    duration,
    preAllocatedVUs,
    maxVUs: Math.max(preAllocatedVUs, preAllocatedVUs * 2),
  },
  volume: {
    executor: 'shared-iterations',
    vus,
    iterations: totalVotes,
    maxDuration: '15m',
  },
};

if (!profiles[profile]) {
  throw new Error(`Perfil desconhecido: ${profile}`);
}

const minimumExpectedVotes = profile === 'smoke'
  ? 10
  : profile === 'load'
    ? rate * durationInSeconds(duration)
    : totalVotes;
const maximumExpectedVotes = profile === 'load'
  ? minimumExpectedVotes + 1
  : minimumExpectedVotes;
const sessionDurationSeconds = profile === 'load'
  ? Math.max(3600, durationInSeconds(duration) + 300)
  : 3600;

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    voting: profiles[profile],
  },
  thresholds: {
    checks: ['rate==1'],
    'http_req_failed{endpoint:voto}': ['rate==0'],
    'http_req_duration{endpoint:voto}': ['p(95)<500', 'p(99)<1000'],
    dropped_iterations: ['count==0'],
    votes_failed: ['count==0'],
    votes_created: [
      `count>=${minimumExpectedVotes}`,
      `count<=${maximumExpectedVotes}`,
    ],
  },
};

export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const pautaResponse = http.post(
    `${baseUrl}/api/v1/pautas`,
    JSON.stringify({
      titulo: `Performance ${runId}`,
      descricao: `Execucao ${profile}`,
    }),
    { headers, tags: { endpoint: 'setup' } },
  );

  if (!check(pautaResponse, { 'pauta criada': (response) => response.status === 201 })) {
    exec.test.abort(`Falha ao criar pauta: HTTP ${pautaResponse.status}`);
  }

  const pautaId = pautaResponse.json('id');
  const sessaoResponse = http.post(
    `${baseUrl}/api/v1/pautas/${pautaId}/sessoes`,
    JSON.stringify({ duracaoSegundos: sessionDurationSeconds }),
    { headers, tags: { endpoint: 'setup' } },
  );

  if (!check(sessaoResponse, { 'sessao aberta': (response) => response.status === 201 })) {
    exec.test.abort(`Falha ao abrir sessao: HTTP ${sessaoResponse.status}`);
  }

  return { pautaId };
}

export default function ({ pautaId }) {
  const iteration = exec.scenario.iterationInTest;
  const response = http.post(
    `${baseUrl}/api/v1/pautas/${pautaId}/votos`,
    JSON.stringify({
      associadoId: `${runId}-${iteration}`,
      opcao: iteration % 2 === 0 ? 'SIM' : 'NAO',
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'voto' },
    },
  );

  const created = check(response, {
    'voto criado': (result) => result.status === 201,
  });

  if (created) {
    votesCreated.add(1);
  } else {
    votesFailed.add(1);
  }

  if (profile === 'smoke') {
    sleep(1);
  }
}

export function teardown({ pautaId }) {
  const response = http.get(
    `${baseUrl}/api/v1/pautas/${pautaId}/resultado`,
    { tags: { endpoint: 'teardown' } },
  );

  check(response, {
    'resultado consultado': (result) => result.status === 200,
    'total persistido confere': (result) => {
      const persistedVotes = Number(result.json('totalVotos'));
      return result.status === 200
        && persistedVotes >= minimumExpectedVotes
        && persistedVotes <= maximumExpectedVotes;
    },
  });
}

export function handleSummary(data) {
  return {
    [`/results/${runId}/summary.json`]: JSON.stringify(data, null, 2),
    stdout: '\nRelatorio estruturado: /results/' + runId + '/summary.json\n',
  };
}

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value || '', 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function durationInSeconds(value) {
  const match = /^(\d+)(s|m|h)$/.exec(value);
  if (!match) {
    throw new Error(`Duracao invalida: ${value}. Use s, m ou h, por exemplo 30s ou 2m.`);
  }

  const multiplier = { s: 1, m: 60, h: 3600 }[match[2]];
  return Number(match[1]) * multiplier;
}
