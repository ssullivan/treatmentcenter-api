

CREATE TABLE feed_detail (
  id UUID primary key NOT NULL,
  is_search_feed boolean NOT NULL default false,
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now())
);

-- This table is used to store a unique list
-- of categories from the SAMSHA data set

create table category (
  id serial primary key,
  code varchar(32) NOT NULL UNIQUE,
  json text NOT NULL default '{}'
);

-- This table is used to store a unique list of
-- services from the SAMSHA data set

create table service (
   id serial primary key,
   code varchar(32) NOT NULL UNIQUE,
   category_code text NOT NULL,
   json text NOT NULL default '{}'
);

-- This table us used to store lat/lon geometry points for postalcodes

create sequence postalcode_id_seq;

-- country code      : iso country code, 2 characters
-- postal code       : varchar(20)
-- place name        : varchar(180)
-- admin name1       : 1. order subdivision (state) varchar(100)
-- admin code1       : 1. order subdivision (state) varchar(20)
-- admin name2       : 2. order subdivision (county/province) varchar(100)
-- admin code2       : 2. order subdivision (county/province) varchar(20)
-- admin name3       : 3. order subdivision (community) varchar(100)
-- admin code3       : 3. order subdivision (community) varchar(20)
-- latitude          : estimated latitude (wgs84)
-- longitude         : estimated longitude (wgs84)
-- accuracy          : accuracy of lat/lng from 1=estimated to 6=centroid
create table postalcode (
  id serial primary key,
  postalcode text NOT NULL,
  placename text,
  admin_name1 text,
  admin_code1 text,
  admin_name2 text,
  admin_code2 text,
  admin_name3 text,
  admin_code3 text,
  latitude float,
  longitude float,
  accuracy int default 1,
  geog GEOMETRY(POINT,4326)
);


create table location (
  id UUID primary key not null,
  feed_id UUID not null,
  name1 text,
  name2 text,
  json text NOT NULL default '{}',
  state text,
  city text,
  postalcode text,
  county text,
  street text,
  lat float,
  lon float,
  geog GEOMETRY(POINT,4326),
  cats int[],
  services int[]
);


CREATE INDEX idx_location_cats ON location USING gin(cats);
CREATE INDEX idx_location_services ON location USING gin(services);
CREATE INDEX idx_location_cats_services ON location USING gin(services, cats);
CREATE INDEX idx_location_postalcode ON location USING btree(feed_id, postalcode);
CREATE INDEX idx_location_gix ON location USING gist(feed_id, geog);
CREATE INDEX idx_postalcode_postalcode ON postalcode USING btree(postalcode);