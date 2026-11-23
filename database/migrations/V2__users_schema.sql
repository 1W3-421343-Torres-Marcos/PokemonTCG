CREATE TABLE users (
    id             UUID PRIMARY KEY,
    username       VARCHAR(50) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20) NOT NULL DEFAULT 'USER',
    matches_played INT NOT NULL DEFAULT 0,
    matches_won    INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_users_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

CREATE TABLE user_favorite_cards (
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    card_id    VARCHAR(64) NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, card_id)
);
