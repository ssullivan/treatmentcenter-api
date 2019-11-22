CREATE TABLE api_key (
  id UUID primary key NOT NULL,
  domain text NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now())
);