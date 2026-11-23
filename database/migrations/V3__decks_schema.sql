CREATE TABLE decks (
    id           UUID PRIMARY KEY,
    owner_id     UUID REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    description  VARCHAR(500),
    is_starter   BOOLEAN NOT NULL DEFAULT FALSE,
    cpu_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    is_valid     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE deck_cards (
    deck_id  UUID NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
    card_id  VARCHAR(64) NOT NULL REFERENCES cards (id),
    quantity INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (deck_id, card_id)
);

CREATE INDEX idx_decks_owner ON decks (owner_id);
CREATE INDEX idx_decks_starter ON decks (is_starter, cpu_eligible);
