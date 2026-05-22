-- Create Keycloak database
CREATE DATABASE IF NOT EXISTS keycloak_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create Keycloak user and grant privileges
CREATE USER IF NOT EXISTS 'keycloak_user'@'%' IDENTIFIED BY 'keycloak_password';
GRANT ALL PRIVILEGES ON keycloak_dev.* TO 'keycloak_user'@'%';
FLUSH PRIVILEGES;

