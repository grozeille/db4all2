# Specifications of the project

## Goal

The project allows users to manipulate data without technical skills, with a no-code interface.

The user can map an external data, CSV/Excel files or SQL tables, to a "Table" in the project, to browse or manipulate.

"Tables" are grouped into "Projects", visibility and permissions are managed at the project's level.

Onced external data are mapped to "tables", user can manipulate data as applying filters, select columns, add custom columns or even join two tables, and save the result as a new "table".

The product must also work in a standalone mode, similar to Metabase, so a user can start it locally on his computer without setting up a dedicated server.

## General information

### UI design

Content of each page are taking all the space, from left to right.
It's using Bootstrap for visual components.

On the top, there's a header which takes all width.
On the top right, there's a drop-down menu to go to personal settings, or sign-out.
When the user is super-administrator, there's also an additional menu "Admin."

When the user has a 403 or 404 or 500 error, he's redirected to a generic error page displaying an error message.

On the top left, there's a breadcrumb. Ex: Projects / project1 / Tables / table1
The user can click on each element to go back to the previous page.

The hierarchy of the website is:

- Authentication Page
- Error page
- User settings
- Admin. page
- Project List
  - Project creation page
  - Project settings
  - Table list
    - Table creation page
    - Table content
    - Table settings

### Runtime modes

There are 2 runtime modes:
- Standalone mode: the application runs locally on a user workstation, with a default local storage folder, a default master key, and access to the local filesystem enabled by default.
- Shared server mode: the application runs on a shared server, local filesystem access is restricted by configuration, uploads are stored in a managed project folder, and secrets must be protected by an explicit master key.

The same UI and API must work in both runtime modes.

### Data engine

DuckDB is the execution engine used to read files, query remote sources, build transformations, and materialize caches.

DuckDB is used for:
- reading CSV, Excel, Parquet and SQL sources
- executing filters, projections, renaming and joins
- reading cached Parquet files
- building non-materialized views

Project metadata, users, permissions, data source definitions, table definitions, view definitions, cache metadata and job metadata are stored in the application database.

For standalone mode, the application database may use an embedded H2 file-based database.

This H2 database is an internal metadata store for the application, not a user-facing datasource type.

### Datasource strategy

For MVP, the only datasource to implement is Local filesystem.

Rationale:
- it is the easiest datasource to test locally and in standalone mode
- it works in GitHub Codespaces because the application runs inside the devcontainer filesystem
- it is the most useful datasource for low-code users working with CSV and Excel files

After the MVP, the priority order is:
1. S3 bucket
2. Azure Storage
3. PostgreSQL
4. CIFS

PostgreSQL may be simulated during development with H2 only for UI flows, but it is not a substitute for real PostgreSQL connectivity tests.

The initial standalone demo can rely on DuckDB reading CSV or Parquet files from a local folder datasource.

For CIFS, the product does not expose the remote files through a custom HTTP or S3 proxy.

Instead, CIFS is handled as a small-volume datasource where the application performs scheduled local copies and then reads the copied data or the derived Parquet cache locally.

For large CSV or Parquet volumes, the recommended datasource types are S3 or Azure Storage.

### Security

There's 2 authentication mode:
- login/password
- SSO with Google or Microsoft

If not authenticated, the user is redirected to the authentication screen.
When authenticated from the authentication screen, the user is redirected to the project list.

For the MVP, project permissions are simplified.

The project administrator can perform all actions in the project.

More granular roles such as read-only may be introduced later.

There's also a "super admin" role. The user for this role can manage login/password, and can be admin role for all projects.

Credentials used to access external systems must never be stored in clear text.

The application receives a MASTER_KEY from the configuration.
- In standalone mode, a default development value may be provided for convenience.
- In shared server mode, the MASTER_KEY must be explicitly configured.

Credentials stored in the application database are encrypted with this MASTER_KEY.

When DuckDB needs to access an external source, the application creates temporary DuckDB secrets from the decrypted credentials.

Secrets must not leak between projects or between users.

DuckDB access is isolated per project. The implementation may rely on one DuckDB connection pool per project.

## User Stories

Check SPECS_PROJECTS.md, SPECS_TABLES.md and SPECS_DATASOURCES.md
