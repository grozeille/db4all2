CREATE TABLE project (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    administrator BOOLEAN
);

CREATE TABLE table_entity (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    project_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project(id)
);
