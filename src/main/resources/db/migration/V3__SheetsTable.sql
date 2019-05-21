--------------------------------------------------------------------------------
-- The following tables are for storing the contents of a Google Spreadsheet
--------------------------------------------------------------------------------

--
-- This table stores high level metadata regarding
--
CREATE TABLE spreadsheet (
  pk bigserial primary key,
  id text NOT NULL,
  name text NOT NULL,
  num_sheets int NOT NULL default 0,
  owner_email text NOT NULL,
  last_editor_email text NOT NULL,
  published boolean NOT NULL default false,
  version bigint NOT NULL DEFAULT 0,
  last_updated TIMESTAMP WITH TIME ZONE NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now())
);


---
--- This table is used to store all changes to the google sheet
--- Every time a Google Spreadsheet is uploaded the version on the sheets_detail will be incremented
--- Every row from the Google Spreadsheet will have this version applied to it
--- If a latitude/longitude are in the row then this will be extracted and stored in the geog field
---
CREATE TABLE worksheet (
  pk bigserial primary key not null,
  spreadsheet_id text NOT NULL,
  id int NOT NULL,
  name text NOT NULL,
  version bigint NOT NULL,
  column_headers text[] NOT NULL,
  row_jsonb jsonb not null default '{}'::jsonb,
  row_index int NOT NULL,
  geog GEOMETRY(POINT,4326) NULL
);

CREATE INDEX idx_sheet_id_version ON worksheet USING btree(version, spreadsheet_id, id, name);
CREATE INDEX idx_sheet_rows_gix ON worksheet USING gist(geog) WHERE geog IS NOT NULL;
CREATE INDEX idx_sheet_row_gin ON worksheet USING gin(row_jsonb);