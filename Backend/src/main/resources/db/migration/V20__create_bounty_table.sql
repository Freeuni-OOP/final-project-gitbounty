CREATE TABLE bounties (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          amount DOUBLE NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          issue_id BIGINT UNIQUE,
                          CONSTRAINT fk_bounty_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE
);