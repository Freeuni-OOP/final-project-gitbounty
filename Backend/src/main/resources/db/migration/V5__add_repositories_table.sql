CREATE TABLE repositories (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              created_at TIMESTAMP(6),
                              updated_at TIMESTAMP(6),
                              description TEXT,
                              name VARCHAR(255) NOT NULL UNIQUE,
                              url VARCHAR(255) NOT NULL
);