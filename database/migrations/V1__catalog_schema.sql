CREATE TABLE card_sets (
    id           VARCHAR(64) PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    series       VARCHAR(100),
    release_date DATE,
    total_cards  INT,
    logo_url     TEXT,
    synced_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE cards (
    id             VARCHAR(64) PRIMARY KEY,
    set_id         VARCHAR(64) NOT NULL REFERENCES card_sets (id),
    local_id       VARCHAR(32) NOT NULL,
    sort_order     INT NOT NULL,
    name           VARCHAR(150) NOT NULL,
    category       VARCHAR(32) NOT NULL,
    hp             INT,
    stage          VARCHAR(32),
    evolve_from    VARCHAR(150),
    retreat        INT,
    trainer_type   VARCHAR(64),
    energy_type    VARCHAR(32),
    rarity         VARCHAR(64),
    illustrator    VARCHAR(150),
    image_low_url  TEXT,
    image_high_url TEXT,
    attacks        JSONB NOT NULL DEFAULT '[]'::jsonb,
    weaknesses     JSONB NOT NULL DEFAULT '[]'::jsonb,
    resistances    JSONB NOT NULL DEFAULT '[]'::jsonb,
    effect_text    TEXT,
    raw_payload    JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE card_types (
    card_id VARCHAR(64) NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    type    VARCHAR(32) NOT NULL,
    PRIMARY KEY (card_id, type)
);

CREATE INDEX idx_cards_set_sort ON cards (set_id, sort_order, id);
CREATE INDEX idx_cards_name_lower ON cards (LOWER(name));
CREATE INDEX idx_cards_category ON cards (category);
CREATE INDEX idx_cards_stage ON cards (stage);
CREATE INDEX idx_card_types_type ON card_types (type);
