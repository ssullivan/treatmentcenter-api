CREATE TABLE api_key (
  id UUID primary key NOT NULL,
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now())
);