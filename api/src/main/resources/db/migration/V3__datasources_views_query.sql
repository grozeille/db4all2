CREATE TABLE "datasources" (
    "id" VARCHAR(255) PRIMARY KEY,
    "project_id" VARCHAR(255) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "description" VARCHAR(1024),
    "type" VARCHAR(64) NOT NULL,
    "read_only" BOOLEAN NOT NULL,
    "configuration_json" CLOB NOT NULL,
    CONSTRAINT "fk_datasource_project" FOREIGN KEY ("project_id") REFERENCES "projects"("id"),
    CONSTRAINT "uk_datasource_project_name" UNIQUE ("project_id", "name")
);

ALTER TABLE "tables"
    ADD COLUMN "datasource_id" VARCHAR(255);

ALTER TABLE "tables"
    ADD COLUMN "source_kind" VARCHAR(64);

ALTER TABLE "tables"
    ADD COLUMN "configuration_json" CLOB;

ALTER TABLE "tables"
    ADD CONSTRAINT "fk_table_datasource" FOREIGN KEY ("datasource_id") REFERENCES "datasources"("id");

CREATE TABLE "views" (
    "id" VARCHAR(255) PRIMARY KEY,
    "project_id" VARCHAR(255) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "description" VARCHAR(1024),
    "type" VARCHAR(64) NOT NULL,
    "source_table_id" VARCHAR(255) NOT NULL,
    "query_json" CLOB NOT NULL,
    "compiled_sql" CLOB NOT NULL,
    CONSTRAINT "fk_view_project" FOREIGN KEY ("project_id") REFERENCES "projects"("id"),
    CONSTRAINT "fk_view_table" FOREIGN KEY ("source_table_id") REFERENCES "tables"("id"),
    CONSTRAINT "uk_view_project_name" UNIQUE ("project_id", "name")
);