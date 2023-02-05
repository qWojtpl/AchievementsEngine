
CREATE TABLE IF NOT EXISTS players (
    id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nick VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS achievements (
    achievement_key VARCHAR(128) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS progress (
    id_player INT,
    achievement_key VARCHAR(128),
    event INT,
    progress INT,
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (achievement_key) REFERENCES achievements(achievement_key)
);

CREATE TABLE IF NOT EXISTS completed (
    id_player INT,
    achievement_key VARCHAR(128),
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (achievement_key) REFERENCES achievements(achievement_key)
);