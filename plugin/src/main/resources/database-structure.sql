
CREATE TABLE IF NOT EXISTS players (
    id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nick VARCHAR(16) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS achievements (
    id_achievement INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    achievement_key VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS progress (
    id_player INT NOT NULL,
    id_achievement INT NOT NULL,
    event INT NOT NULL,
    progress INT NOT NULL,
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement),
    UNIQUE(id_player, id_achievement, event)
);

CREATE TABLE IF NOT EXISTS completed (
    id_player INT NOT NULL,
    id_achievement INT NOT NULL,
    FOREIGN KEY (id_player) REFERENCES players(id_player),
    FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement),
    UNIQUE(id_player, id_achievement)
);