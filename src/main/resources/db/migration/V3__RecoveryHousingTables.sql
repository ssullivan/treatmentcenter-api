--------------------------------------------------------------------------------
-- The following tables are for storing the contents of a Google Spreadsheet
--------------------------------------------------------------------------------

--
-- feed_name is a unique name for the source of the address
-- feed_record_id is a unique numeric identifier from the feed
-- serves is a list of groups/categories of people Male/Female/Co-Ed/Etc..
-- feed_version is the system time in millis

CREATE TABLE recovery_housing (
    id bigserial primary key,

    feed_name text not null,
    feed_record_id bigint not null,
    feed_version bigint not null,

    name text not null,
    contact_name text null,
    contact_email text null,
    contact_phonenumber text null,
    capacity int not null default 0,
    fee int not null default 0,
    wehsite text null,
    serves text[],
    state text,
    city text,
    postalcode text,
    county text,
    street text,
    lat float not null default 0,
    lon float not null default 0,
    geog GEOMETRY(POINT,4326),

    UNIQUE(id, feed_name, feed_record_id)
)