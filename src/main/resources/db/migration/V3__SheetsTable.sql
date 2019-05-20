--------------------------------------------------------------------------------
-- The following tables are for storing the contents of a Google Spreadsheet
--------------------------------------------------------------------------------

--
-- This table stores high level metadata regarding
--
CREATE TABLE sheet_detail (
  id bigserial primary key,
  sheet_id text NOT NULL,
  name text NOT NULL,
  last_editor_email text NOT NULL,
  rows int NOT NULL DEFAULT 0,
  cols int NOT NULL DEFAULT 0,
  version bigint NOT NULL DEFAULT 0,
  last_updated TIMESTAMP WITH TIME ZONE NULL,
  fields text[] NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now())
);


---
--- This table is used to store all changes to the google sheet
--- Every time a Google Spreadsheet is uploaded the version on the sheets_detail will be incremented
--- Every row from the Google Spreadsheet will have this version applied to it
--- If a latitude/longitude are in the row then this will be extracted and stored in the geog field
---

CREATE TABLE sheet_rows (
  id bigserial primary key not null,
  sheet_id text NOT NULL,
  version bigint NOT NULL,
  row jsonb not null default '{}'::jsonb,
  row_index int NOT NULL,
  geog GEOMETRY(POINT,4326) NULL
);

CREATE INDEX idx_sheet_id_version ON sheet_rows USING btree(version, sheet_id);
CREATE INDEX idx_sheet_rows_gix ON sheet_rows USING gist(version, sheet_id, geog) WHERE geog IS NOT NULL;
CREATE INDEX idx_sheet_row_gin ON sheet_rows USING gin(row);