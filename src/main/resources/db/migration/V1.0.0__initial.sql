CREATE TABLE IF NOT EXISTS user (
    uid int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(55) UNIQUE KEY
);

CREATE TABLE IF NOT EXISTS class_properties (
    pid INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    uri VARCHAR(255) NOT NULL,
    uid INT NOT NULL,
    preference DOUBLE NOT NULL DEFAULT 1.0,
    confidence DOUBLE NOT NULL DEFAULT 0.5,
    CONSTRAINT props_user_fk FOREIGN KEY (uid)
        REFERENCES user(uid)
        ON DELETE CASCADE,
    CONSTRAINT unique_uri_uid UNIQUE(uri, uid)
);

CREATE TABLE IF NOT EXISTS context_factor (
    cid INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS relevance (
    rid INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cid INT NOT NULL,
    uid INT NOT NULL,
    uri VARCHAR(255) NOT NULL,
    value DOUBLE NOT NULL CHECK (0 <= value AND value <= 2),
    CONSTRAINT unique_cid_uri UNIQUE(cid, uri, uid),
    CONSTRAINT rel_factor_fk FOREIGN KEY(cid)
        REFERENCES context_factor(cid)
        ON DELETE CASCADE,
    CONSTRAINT rel_user_fk FOREIGN KEY(uid)
        REFERENCES user(uid)
        ON DELETE CASCADE
)