-- Hand-written H2 schema for CodebaseDeletionServicePersistenceTests.
--
-- We don't let Hibernate generate the schema (hbm2ddl) here: Hibernate 7 emits an
-- automatic CHECK (col IN (...)) constraint for @Enumerated(STRING) columns, and the
-- H2 version currently pulled in has a bug evaluating that constraint (throws
-- "The database has been closed" from inside ConditionInConstantSet.getValue),
-- unrelated to the persistence-context bug this test suite exists to catch.
-- Hand-rolling a minimal, MySQL-migration-equivalent schema sidesteps it entirely.

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    credit_balance DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE codebases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    git_url VARCHAR(2048) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codebase_id BIGINT NOT NULL REFERENCES codebases(id),
    commit_hash VARCHAR(40) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    message CLOB NOT NULL,
    committed_at TIMESTAMP NOT NULL,
    CONSTRAINT uc_codebase_with_commit UNIQUE (codebase_id, commit_hash)
);

CREATE TABLE branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codebase_id BIGINT NOT NULL REFERENCES codebases(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    latest_commit_id BIGINT REFERENCES commits(id),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uc_branch_in_codebase UNIQUE (codebase_id, name)
);

CREATE TABLE issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description CLOB,
    status VARCHAR(255) NOT NULL,
    author_id BIGINT NOT NULL REFERENCES users(id),
    assigned_to_id BIGINT REFERENCES users(id),
    repository_id BIGINT NOT NULL REFERENCES codebases(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    type VARCHAR(31) NOT NULL DEFAULT 'ISSUE'
);

CREATE TABLE pull_request (
    id BIGINT PRIMARY KEY REFERENCES issues(id),
    source_branch_id BIGINT REFERENCES branches(id),
    target_branch_id BIGINT NOT NULL REFERENCES branches(id),
    merged_at TIMESTAMP
);

CREATE TABLE bounties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description CLOB,
    amount DOUBLE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    issue_id BIGINT UNIQUE REFERENCES issues(id) ON DELETE CASCADE
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT REFERENCES users(id),
    to_user_id BIGINT REFERENCES users(id),
    bounty_id BIGINT REFERENCES bounties(id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP
);

CREATE TABLE codebase_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repo_id BIGINT NOT NULL REFERENCES codebases(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT uc_codebase_member UNIQUE (repo_id, user_id)
);
