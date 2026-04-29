# Specifications related to data sources, cache and views

## Scope

This document defines:
- datasource management
- physical tables mapped on top of datasources
- cache behavior
- refresh queue and job execution
- logical views built on top of tables

Related draft OpenAPI specification:
- api/src/main/resources/static/openapi/datasources-draft.openapi.yaml

## Datasource types

### MVP datasource

The first datasource to implement is Local filesystem.

For the MVP, it is also the only datasource exposed to the user.

It allows users to read files directly from the machine running the application.

In standalone mode, Local filesystem is enabled by default.

In shared server mode, Local filesystem access is controlled by configuration:
- either the user can browse all the server filesystem
- or the user is restricted to a managed project folder

### Planned datasource types

The product must support the following datasource types:
- Local filesystem
- S3 bucket
- Azure Storage
- PostgreSQL database
- CIFS folder

The implementation priority after MVP is:
1. S3 bucket
2. Azure Storage
3. PostgreSQL database
4. CIFS folder

The standalone application metadata may use H2 internally, but H2 is not a datasource type exposed to users.

## Datasource management UI

Data sources are managed from the project settings page.

The page displays a "Data sources" section with:
- a table listing all datasources of the project
- one row per datasource, with name, type, read-only status, cache default policy summary, and action buttons
- action buttons: Configure, Remove
- a top-right button: Add data source

### Add datasource flow

When the user clicks "Add data source", a modal or dedicated form is displayed.

Common fields for all datasource types:
- datasource name
- datasource type
- read-only flag when relevant
- optional description
- a "Test connection" button when relevant
- Save and Cancel buttons

If the datasource configuration is invalid or the connection test fails, the UI displays a clear error message and Save is blocked.

For the MVP UI, only Local filesystem is shown in the datasource type list.

### Local filesystem datasource

Fields:
- name
- root path
- read-only flag

Behavior:
- in standalone mode, the user may browse the local filesystem if configuration allows it
- in shared server mode, the user may only browse the allowed managed folder when global filesystem access is disabled
- for the MVP, the connection test succeeds when the configured folder exists and the application can list its files

### S3 datasource

Fields:
- name
- endpoint / hostname
- port when needed
- bucket
- base path prefix
- access key
- secret key
- region if needed
- read-only flag
- optional advanced connection options

### Azure Storage datasource

Fields:
- name
- account name
- container
- base path prefix
- credential type
- shared key or SAS token depending on the credential type
- read-only flag

### PostgreSQL datasource

Fields:
- name
- hostname
- port
- database name
- schema filter
- login
- password
- optional connection properties with key/value syntax

### CIFS datasource

Fields:
- name
- UNC path or server/share path
- login
- password
- read-only flag

Behavior:
- CIFS is intended for small files and small volumes, especially Excel and small CSV files
- CIFS is not a direct connection to the remote file for analytical reads
- the application works through a file copy / synchronization workflow into managed local storage
- the user configures a copy frequency, and reads are performed from the synchronized local copy or from the Parquet cache derived from it
- CIFS is not suitable for large files or large-scale analytical scans
- for large CSV or Parquet datasets, the recommended datasource types are S3 or Azure Storage instead of CIFS

## Uploads

When the datasource allows writing, the user can upload files from the UI.

This is especially important in shared server mode for users who do not have S3, Azure Storage or CIFS access.

Uploads are stored in a local managed folder on the machine running the application.

Storage rules:
- one root upload folder configured in application settings
- one subfolder per project
- the project folder may contain datasource-specific subfolders

The Local filesystem datasource may point to this managed folder.

## Physical tables

A project table is a logical object created by the user and mapped to a physical source.

Its role is to provide a stable UI abstraction over a file or external table.

The user does not query a datasource directly. The user creates tables from datasources, then uses these tables in the UI.

Supported physical table source kinds:
- CSV file or file pattern
- Parquet files

For the MVP, only CSV and Parquet mapped from a Local filesystem datasource are in scope.

Excel workbook support and PostgreSQL table support remain future work.

Future support may include Iceberg, but it is not part of MVP.

## Table configuration from datasource

### CSV table

Required fields:
- table name
- datasource
- path or file pattern

Optional fields:
- separator
- quote character
- encoding
- first row as header
- null value handling

### Excel table

Required fields:
- table name
- datasource
- file path

Optional fields:
- sheet name
- named range or explicit range
- first row as header

The UI must provide a scan action to list sheets or named ranges.

For CIFS-backed Excel files, the scan is performed on the latest local copied version of the file.

### PostgreSQL table

Required fields:
- table name
- datasource
- schema and table name, or relation name

The UI must provide a scan action to list accessible schemas and tables.

## Query experience

The target users are low-code users.

The UI must provide a query builder to filter and explore data without requiring SQL knowledge.

The result grid must support:
- fast rendering of large result sets
- copy of selected cells
- paste into Excel or similar spreadsheet tools

For the MVP UI, the grid displays at most 1000 rows for a query preview.

The API does not provide a total row count or a hasMore flag for query previews.

If the user needs the full result of a query, the product provides a dedicated CSV export action instead of relying on grid pagination metadata.

Handsontable is an acceptable option for the result grid if it provides the needed clipboard and spreadsheet-like interactions.

## Cache

### Goal

Reading Excel files or remote sources may be slow.

Each physical table may therefore define an optional cache.

The cache is a materialized copy of the source data stored as local Parquet files.

For CIFS-backed tables, the copy from CIFS to local storage is part of the source ingestion strategy.

This means there are 2 distinct concepts:
- source copy: replication of the remote CIFS file into managed local storage
- analytical cache: Parquet materialization used by DuckDB for query performance

For CIFS, the source copy is mandatory.
For other datasource types, the source copy is not mandatory.

### Cache configuration

In the table settings UI, the user can:
- enable or disable cache
- choose the refresh schedule
- choose whether reading must prefer cache when cache exists
- invalidate the cache
- delete the cache
- force a refresh now

For CIFS-backed tables, the user can also configure the frequency of the source copy.

The default simple policy is:
- copy every X hours
- or copy every X days

Advanced cron scheduling may also be supported.

### Scheduling modes

The UI supports 3 scheduling modes:
- every X hours starting from hour H
- every X days starting from a given date/time
- advanced cron expression

The simple modes are the recommended default UI.

### Cache metadata

Cache metadata is stored in the application database.

For each cached table, metadata includes at least:
- cache enabled flag
- current cache version
- previous cache version if switch is in progress
- cache status
- last refresh start date
- last refresh end date
- last successful refresh date
- last failed refresh date
- last failure message
- schedule definition
- manual invalidation flag

### Cache lifecycle

Cache refresh is versioned.

If version V1 is active and a refresh starts, the job builds V2 in isolation.

Rules:
- readers continue using V1 while V2 is not complete
- when V2 is complete, the metadata atomically switches the active cache from V1 to V2
- V1 is deleted asynchronously after the switch
- if V2 creation fails, V1 stays active

For CIFS-backed tables, the refresh sequence is:
1. copy the remote source file to a new local source version
2. build a new Parquet cache version from that local source version if cache is enabled
3. switch readers atomically to the new active version when processing is complete

If the cache is invalidated:
- the current active cache version is removed
- subsequent queries read from the real source until a new refresh completes

If the cache is deleted entirely:
- deletion is asynchronous
- while deletion is pending, the UI displays that the cache is being removed
- reads fall back to the source directly once the active cache is no longer valid

### Last refresh information

When the user opens a table, the UI displays cache information including:
- whether the cache is enabled
- whether the current query reads from cache or source
- the date/time of the last successful refresh
- the current refresh status if a job is running

## Refresh queue and jobs

Cache refreshes and cache deletions are asynchronous jobs.

The application maintains a queue of jobs to avoid starting all heavy refreshes at the same time.

### Queue rules

- jobs are persisted in the database
- jobs belong to a project and to a table when relevant
- only a limited number of jobs run concurrently
- jobs are picked from the queue by a worker process

### Job types

Supported job types:
- copy source from CIFS
- refresh cache
- delete cache
- upload file if upload is asynchronous

### Queue UI

From the project, the user can open a queue view displaying:
- queued jobs
- running jobs
- completed jobs
- failed jobs

For each job, the UI shows:
- job type
- related table
- current status
- creation date
- start date
- end date
- progress summary when available
- error message when failed

Actions:
- cancel one queued or running job
- cancel all queued jobs for the project
- optionally retry a failed job

## Views

### Goal

Users must be able to create logical views on top of physical tables.

Views are not materialized caches. They are logical definitions executed by DuckDB at query time.

### Supported view types

The first supported view capabilities are:
- saved filters
- select columns
- rename columns
- join 2 sources

### Filter view

The user can save:
- filters
- column selection
- column renaming

The result is a named view visible in the table list.

### Join view

The user can create a view by joining 2 existing tables or views.

The UI allows the user to define:
- left source
- right source
- join type
- join conditions
- selected columns in the result
- optional renamed result columns

### View limits

Views do not have their own cache.

DuckDB execution is expected to be sufficient for these logical views.

If performance becomes a problem later, the product may allow materializing a view as a new physical table, but this is not part of MVP.

## Permissions

Datasources, tables, caches and views belong to a project.

Project permissions apply to these objects.

For the MVP, permissions are simplified:
- project administrators can perform all actions in the project
- read-only or writer-specific roles are not yet implemented

## Non-functional expectations

- the product must be easy to start locally with minimal configuration
- the product must behave correctly inside GitHub Codespaces and devcontainers
- the product must not require the end user to know SQL for common operations
- secrets must be isolated per project and never leaked in logs or to other users
- the CIFS implementation must remain portable and must not rely on OS-level mount commands

## API endpoints

This section defines the target API contract for datasources, physical tables, source copies, cache, jobs and views.

All endpoints are under `/api/v2`.

All endpoints return JSON except file upload endpoints.

All error responses return a JSON payload with at least:
- `message`
- `errorType`

Typical status codes:
- `200` for successful read or action returning a body
- `201` for successful creation if the implementation chooses to distinguish creation from update
- `204` for successful deletion without response body
- `400` for invalid input
- `401` for unauthenticated access
- `403` for insufficient project permissions
- `404` for missing project, datasource, table, view or job
- `409` for conflicts such as duplicate name or incompatible state

### Permissions model for the API

- readers can read projects, tables, views and their metadata
- writers can create tables and views, and trigger data refresh actions
- project administrators can manage datasources, uploads, source-copy policies, cache policies and project job queue actions

### Datasource DTO

Datasource responses expose at least:
- `id`
- `name`
- `type`
- `description`
- `readOnly`
- `projectId`
- `status`
- `lastConnectionTestAt`
- `lastConnectionTestStatus`
- `configurationSummary`

Sensitive credentials are never returned.

### Datasource endpoints

#### List datasources of a project

- `GET /projects/{projectId}/datasources`

Response:
- list of datasource DTOs for the project

Permission:
- reader or above

#### Get one datasource

- `GET /projects/{projectId}/datasources/{datasourceId}`

Response:
- datasource DTO with full editable non-secret configuration metadata

Permission:
- reader or above

#### Create datasource

- `POST /projects/{projectId}/datasources`

Request body:
- `name`
- `type`
- `description`
- `readOnly`
- `configuration`

The `configuration` object depends on the datasource type.

Response:
- created datasource DTO

Permission:
- project administrator

#### Update datasource

- `PUT /projects/{projectId}/datasources/{datasourceId}`

Request body:
- same structure as create, with only editable fields

Response:
- updated datasource DTO

Permission:
- project administrator

#### Delete datasource

- `DELETE /projects/{projectId}/datasources/{datasourceId}`

Behavior:
- deletion is rejected if tables still depend on the datasource unless a future force-delete mode is explicitly added

Permission:
- project administrator

#### Test datasource connection

- `POST /projects/{projectId}/datasources/{datasourceId}/test-connection`

Response:
- `status`
- `message`
- optional technical details safe to display to the user

Permission:
- project administrator

#### Browse datasource content

- `POST /projects/{projectId}/datasources/{datasourceId}/browse`

Purpose:
- list folders, files, schemas, tables or other navigable objects depending on datasource type

Request body:
- `path` or datasource-specific browse cursor

Response:
- list of entries with name, type, path and metadata useful to the UI

Permission:
- writer or above

#### Scan datasource content for table creation

- `POST /projects/{projectId}/datasources/{datasourceId}/scan`

Purpose:
- inspect a file or remote source to return available sheets, ranges, tables, schemas or file matches

Request body examples:
- Excel: `path`
- PostgreSQL: optional `schemaFilter`
- Local filesystem / S3 / Azure / CIFS: `path` or `glob`

Response:
- datasource-type-specific scan result

Permission:
- writer or above

### Upload endpoints

#### Upload a file into managed project storage

- `POST /projects/{projectId}/uploads`

Request:
- multipart upload
- target folder metadata

Response:
- uploaded file metadata, including managed local path or logical project-relative path

Permission:
- project administrator

#### List uploaded files of the project

- `GET /projects/{projectId}/uploads`

Response:
- list of uploaded file metadata

Permission:
- writer or above

### Physical table DTO

Physical table responses expose at least:
- `id`
- `projectId`
- `name`
- `description`
- `sourceKind`
- `datasourceId`
- `configuration`
- `cache`
- `lastRefreshAt`
- `lastRefreshStatus`
- `usingCache`

### Physical table endpoints

#### Create physical table

- `POST /projects/{projectId}/tables`

Request body:
- `name`
- `description`
- `sourceKind`
- `datasourceId`
- `configuration`
- optional initial cache settings

Response:
- created table DTO

Permission:
- writer or above

#### Update physical table configuration

- `PUT /projects/{projectId}/tables/{tableId}`

Request body:
- editable fields of the table configuration

Response:
- updated table DTO

Permission:
- writer or above

#### Delete physical table

- `DELETE /projects/{projectId}/tables/{tableId}`

Behavior:
- may create asynchronous cleanup jobs for cache deletion or source-copy cleanup

Permission:
- writer or above

#### Preview physical table configuration result

- `POST /projects/{projectId}/tables/preview`

Purpose:
- validate source configuration and preview columns / sample rows before saving the table

Request body:
- same payload as create table, without persisted identifiers

Response:
- inferred schema
- sample rows
- warnings

Permission:
- writer or above

### Table data endpoints

#### Read table content

- `POST /projects/{projectId}/tables/{tableId}/query`

Purpose:
- execute filters, aggregation, column projection, renaming and custom expressions against the table

Request body:
- pagination
- sorting
- filters
- aggregation
- visible columns
- renamed columns
- computed columns

Response:
- column metadata
- rows
- execution metadata
- `sourceMode` indicating `SOURCE` or `CACHE`
- cache freshness metadata when relevant

Permission:
- reader or above

#### Get table metadata

- `GET /projects/{projectId}/tables/{tableId}/metadata`

Response:
- schema
- source information
- cache status
- latest successful refresh timestamp

Permission:
- reader or above

### Source-copy endpoints

These endpoints mainly apply to CIFS-backed tables.

#### Get source-copy policy

- `GET /projects/{projectId}/tables/{tableId}/source-copy`

Response:
- source-copy enabled status
- scheduling policy
- latest copy metadata
- latest copy job status

Permission:
- reader or above

#### Update source-copy policy

- `PUT /projects/{projectId}/tables/{tableId}/source-copy`

Request body:
- `enabled`
- scheduling mode and values
- retention policy if relevant

Response:
- updated source-copy policy DTO

Permission:
- project administrator

#### Trigger source copy now

- `POST /projects/{projectId}/tables/{tableId}/source-copy/run-now`

Response:
- created job DTO

Permission:
- writer or above

#### Invalidate current source copy

- `POST /projects/{projectId}/tables/{tableId}/source-copy/invalidate`

Behavior:
- marks the current copied source as invalid and schedules asynchronous cleanup if needed

Permission:
- project administrator

### Cache endpoints

#### Get cache configuration and status

- `GET /projects/{projectId}/tables/{tableId}/cache`

Response:
- cache enabled flag
- schedule
- current active cache version
- latest job metadata
- last successful refresh metadata
- current cache status

Permission:
- reader or above

#### Update cache policy

- `PUT /projects/{projectId}/tables/{tableId}/cache`

Request body:
- `enabled`
- `preferCache`
- schedule configuration
- retention policy if relevant

Response:
- updated cache DTO

Permission:
- project administrator

#### Trigger cache refresh now

- `POST /projects/{projectId}/tables/{tableId}/cache/refresh`

Behavior:
- creates a refresh job
- if the table is CIFS-backed, the refresh job includes source copy then Parquet cache build

Response:
- created job DTO

Permission:
- writer or above

#### Invalidate cache

- `POST /projects/{projectId}/tables/{tableId}/cache/invalidate`

Behavior:
- marks the active cache as invalid without immediately deleting metadata needed for the UI

Permission:
- project administrator

#### Delete cache

- `DELETE /projects/{projectId}/tables/{tableId}/cache`

Behavior:
- schedules asynchronous deletion of active and obsolete cache files

Permission:
- project administrator

### Job DTO

Job responses expose at least:
- `id`
- `projectId`
- `tableId`
- `type`
- `status`
- `createdAt`
- `startedAt`
- `endedAt`
- `progress`
- `message`
- `cancelable`

### Job endpoints

#### List project jobs

- `GET /projects/{projectId}/jobs`

Query parameters may include:
- `status`
- `type`
- `page`
- `size`

Response:
- paginated list of job DTOs

Permission:
- writer or above

#### Get one job

- `GET /projects/{projectId}/jobs/{jobId}`

Response:
- job DTO with execution details

Permission:
- writer or above

#### Cancel one job

- `POST /projects/{projectId}/jobs/{jobId}/cancel`

Behavior:
- cancels a queued job
- requests cooperative cancellation for a running job when supported

Permission:
- project administrator

#### Cancel all queued jobs of a project

- `POST /projects/{projectId}/jobs/cancel-all`

Request body may include:
- optional job type filter

Permission:
- project administrator

### View DTO

View responses expose at least:
- `id`
- `projectId`
- `name`
- `description`
- `type`
- `definition`
- `baseTables`
- `createdAt`
- `updatedAt`

### View endpoints

#### List views of a project

- `GET /projects/{projectId}/views`

Permission:
- reader or above

#### Get one view

- `GET /projects/{projectId}/views/{viewId}`

Permission:
- reader or above

#### Create view

- `POST /projects/{projectId}/views`

Request body:
- `name`
- `description`
- `type`
- `definition`

Supported initial view types:
- filter/projection view
- join view

Permission:
- writer or above

#### Update view

- `PUT /projects/{projectId}/views/{viewId}`

Permission:
- writer or above

#### Delete view

- `DELETE /projects/{projectId}/views/{viewId}`

Permission:
- writer or above

#### Query view content

- `POST /projects/{projectId}/views/{viewId}/query`

Behavior:
- same query contract as physical tables
- views never use a dedicated cache of their own

Permission:
- reader or above

### Recommended implementation order for the API

1. Datasource CRUD for Local filesystem
2. Physical table create/update/delete for CSV and Excel
3. Table preview and table query endpoint
4. Cache endpoints for non-CIFS sources
5. Job queue endpoints
6. CIFS datasource and source-copy endpoints
7. View endpoints
8. S3, Azure Storage and PostgreSQL advanced scan/browse features