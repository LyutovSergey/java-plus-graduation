CREATE TABLE IF NOT EXISTS user_actions (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL,
                                            event_id BIGINT NOT NULL,
                                            weight DOUBLE PRECISION NOT NULL,
                                            last_action_time TIMESTAMP NOT NULL,
                                            CONSTRAINT unique_user_event UNIQUE (user_id, event_id)
    );

CREATE TABLE IF NOT EXISTS event_similarities (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  event_a BIGINT NOT NULL,
                                                  event_b BIGINT NOT NULL,
                                                  score DOUBLE PRECISION NOT NULL,
                                                  updated_at TIMESTAMP NOT NULL,
                                                  CONSTRAINT unique_pair UNIQUE (event_a, event_b),
    CONSTRAINT check_a_less_b CHECK (event_a < event_b)
    );

CREATE INDEX IF NOT EXISTS idx_user_actions_user_id ON user_actions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_event_id ON user_actions(event_id);
CREATE INDEX IF NOT EXISTS idx_event_similarities_a ON event_similarities(event_a);
CREATE INDEX IF NOT EXISTS idx_event_similarities_b ON event_similarities(event_b);