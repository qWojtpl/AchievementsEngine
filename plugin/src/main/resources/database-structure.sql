
CREATE TABLE IF NOT EXISTS players (
    id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nick VARCHAR(16)
);

CREATE TABLE IF NOT EXISTS achievements (
    id_achievement INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    achievement_key VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS progress (
    id_player INT,
    id_achievement INT,
    event INT,
    progress INT,
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)
);

CREATE TABLE IF NOT EXISTS completed (
    id_player INT,
    id_achievement INT,
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)
);