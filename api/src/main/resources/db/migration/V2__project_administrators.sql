CREATE TABLE "project_administrators" (
    "project_id" VARCHAR(255) NOT NULL,
    "user_email" VARCHAR(255) NOT NULL,
    CONSTRAINT "pk_project_administrators" PRIMARY KEY ("project_id", "user_email"),
    CONSTRAINT "fk_project_administrators_project" FOREIGN KEY ("project_id") REFERENCES "projects"("id") ON DELETE CASCADE,
    CONSTRAINT "fk_project_administrators_user" FOREIGN KEY ("user_email") REFERENCES "users"("email") ON DELETE CASCADE
);

INSERT INTO "project_administrators" ("project_id", "user_email")
SELECT p."id", u."email"
FROM "projects" p
CROSS JOIN "users" u
WHERE u."super_admin" = TRUE;

ALTER TABLE "projects" DROP COLUMN "administrator";