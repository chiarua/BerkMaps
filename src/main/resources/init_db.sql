CREATE DATABASE IF NOT EXISTS bearmaps;
USE bearmaps;

CREATE TABLE IF NOT EXISTS locations (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;