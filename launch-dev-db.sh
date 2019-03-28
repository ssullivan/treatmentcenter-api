#!/bin/sh
docker run --name app_postgis -p 5432:5432  -e POSTGRES_DB=app_dev -e POSTGRES_PASSWORD= -d mdillon/postgis:10

