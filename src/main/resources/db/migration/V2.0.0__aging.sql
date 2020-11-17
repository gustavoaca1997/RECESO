CREATE TABLE IF NOT EXISTS aging (
    aid INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    puri VARCHAR(255) NOT NULL,
    uid INT NOT NULL,
    value DOUBLE NOT NULL,
    CONSTRAINT unique_puri_uid UNIQUE(puri, uid),
    CONSTRAINT ag_user_fk FOREIGN KEY(uid)
        REFERENCES user(uid)
        ON DELETE CASCADE
)