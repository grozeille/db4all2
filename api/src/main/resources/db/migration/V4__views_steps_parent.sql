ALTER TABLE "views"
    ADD COLUMN "parent_view_id" VARCHAR(255);

ALTER TABLE "views"
    ADD COLUMN "steps_json" CLOB;

ALTER TABLE "views"
    ADD CONSTRAINT "fk_view_parent_view" FOREIGN KEY ("parent_view_id") REFERENCES "views"("id");
