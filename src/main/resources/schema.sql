CREATE TABLE IF NOT EXISTS participants (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    community VARCHAR(60) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS missions (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('ambiental', 'social', 'governanca')),
    points INTEGER NOT NULL CHECK (points BETWEEN 1 AND 10000),
    requires_evidence BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS evidence (
    id VARCHAR(36) PRIMARY KEY,
    mission_id VARCHAR(36) NOT NULL REFERENCES missions(id),
    participant_id VARCHAR(36) NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    evidence_text VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('em-analise', 'aprovada', 'rejeitada')),
    cycle_day DATE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    points_snapshot INTEGER NOT NULL CHECK (points_snapshot BETWEEN 1 AND 10000),
    awarded_points INTEGER NOT NULL DEFAULT 0 CHECK (awarded_points >= 0),
    UNIQUE (mission_id, participant_id, cycle_day)
);

CREATE TABLE IF NOT EXISTS friendships (
    participant_id VARCHAR(36) NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    friend_id VARCHAR(36) NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    PRIMARY KEY (participant_id, friend_id),
    CHECK (participant_id <> friend_id)
);

CREATE INDEX IF NOT EXISTS evidence_ranking_idx ON evidence(status, cycle_day, participant_id);
