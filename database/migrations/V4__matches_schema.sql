CREATE TABLE matches (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    player_deck_id UUID REFERENCES decks(id) ON DELETE SET NULL,
    cpu_deck_id UUID REFERENCES decks(id) ON DELETE SET NULL,
    player_deck_name VARCHAR(120) NOT NULL,
    cpu_deck_name VARCHAR(120) NOT NULL,
    bot_difficulty VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    winner VARCHAR(20),
    win_reason VARCHAR(40),
    turn_number INT NOT NULL DEFAULT 0,
    state_json JSONB NOT NULL,
    event_seq BIGINT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matches_user_status ON matches(user_id, status, updated_at DESC);
