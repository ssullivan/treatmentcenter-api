# treatmentcenter-api OpenAPI Specification
[![Build Status](https://travis-ci.org/ssullivan/treatmentcenter-api.svg?branch=master)](https://travis-ci.org/ssullivan/treatmentcenter-api)
[![codecov](https://codecov.io/gh/ssullivan/treatmentcenter-api/branch/master/graph/badge.svg)](https://codecov.io/gh/ssullivan/treatmentcenter-api)
[![Maintainability](https://api.codeclimate.com/v1/badges/e81c336e10d82fa22662/maintainability)](https://codeclimate.com/github/ssullivan/treatmentcenter-api/maintainability)
## Links

- Documentation(ReDoc): https://docs.centerlocator.org/
- SwaggerUI: https://docs.centerlocator.org/swagger-ui/
- Look full spec:
    + JSON https://docs.centerlocator.org/swagger.json
    + YAML https://docs.centerlocator.org/swagger.yaml
- Preview spec version for branch `[branch]`: https://docs.centerlocator.org/preview/[branch]
- API URL: https://api.centerlocator.org

**Warning:** All above links are updated only after Travis CI finishes deployment

## Working on specification
### Install

1. Install [Node JS](https://nodejs.org/)
2. Clone repo and `cd`
    + Run `npm install`

### Usage

1. Run `npm start`
2. Checkout console output to see where local server is started. You can use all [links](#links) (except `preview`) by replacing https://docs.centerlocator.org/treatmentcenter-api/ with url from the message: `Server started <url>`
3. Make changes using your favorite editor or `swagger-editor` (look for URL in console output)
4. All changes are immediately propagated to your local server, moreover all documentation pages will be automagically refreshed in a browser after each change
**TIP:** you can open `swagger-editor`, documentation and `swagger-ui` in parallel
5. Once you finish with the changes you can run tests using: `npm test`
6. Share you changes with the rest of the world by pushing to GitHub :smile:

## Build the image

The build script uses the following environment variables to create the URL for the ECR registry URL.
The Dockerfile it uses for building the image is [here](https://github.com/ssullivan/treatmentcenter-api/blob/master/docker/Dockerfile).

* AWS_ACCOUNT_ID
* AWS_DEFAULT_REGION

The `buildImage` task will create 3 images locally
* `$AWS_ACCOUNT_ID`.dkr.ecr.`$AWS_DEFAULT_REGION`.amazonaws.com/treatmentcenter-api
* `$AWS_ACCOUNT_ID`.dkr.ecr.`$AWS_DEFAULT_REGION`.amazonaws.com/dev/treatmentcenter-api
* test/treatmentcenter-api

```
./gradlew buildImage
```

## Push the images to ECR
```
eval $(aws ecr get-login --no-include-email --region $AWS_DEFAULT_REGION) 
./gradlew pushImageProd
./gradlew pushImageDev
```


## Configuration of Dropwizard Container

The dropwizard web application is configured to listen on port 8080 for its primary rest services and 8081 for its admin services.
The image is designed to be configured purely through environment variables. The following is an exhaustive list of all of the different
settings that can be configured via environment variables.

### Environment Variables
| Env | Default | Description |
| --- | ------- | ----------- |
| CORS_ALLOWED_ORIGINS | https?://.*.centerlocator.org | Controls what domains are allowed to hit the service |
| ENVIRONMENT | - | Controls what domain is used in the swagger docs |
| POSTALCODES_US_PATH | /treatmentcenter-api-latest/data/US.txt | A list of lat/lon for postcal codes | 
| PG_HOST | localhost | the ip or fqdn of the postgres server |
| PG_PORT | 5432 | the port that postgres is listening on |
| PG_USER | postgres | the username to connect to postgres as |
| PG_PASSWORD | - | the password to use when authenticating to postgres |
| PG_DB | app_dev | the name of the database to connect to |
| PG_IAM_AUTH | true | this setting is mututally exclusive with PG_PASSWORD |
| AWS_REGION | us-east-1 | this setting is required when using IAM AUTH |
| PG_SSL | true | this enforces ssl connections to the postgres server |

## Running the Data Collection task

The data [collection task](https://github.com/ssullivan/treatmentcenter-api/blob/master/src/main/java/com/github/ssullivan/tasks/feeds/LoadSamshaCommandPostgres.java) for fetching the SAMSHA data is configured as a Dropwizard command. The name of the command is
`load-samsha-postgres`. The source for parsing the spreadhseet is [TransformLocatorSpreadsheet.java](https://github.com/ssullivan/treatmentcenter-api/blob/master/src/main/java/com/github/ssullivan/tasks/feeds/TransformLocatorSpreadsheet.java)

```bash
docker run --it test/treatmentcenter-api /treatmentcenter-api-latest/bin/treatmentcenter-api load-samsha-postgres 
```

It has the following command line flags:
```bash
usage: java -jar project.jar load-samsha-postgres [-f FILE] [-u URL]
                             [-a ACCESSKEY] [-s SECRETKEY] [-b BUCKET]
                             [-r REGION] [-e ENDPOINT] [--host HOST]
                             [-p PORT] [-d DATABASE] [-U USERNAME]
                             [-P PASSWORD] [-useIAM {true,false}]
                             [-skipS3 {true,false}] [-ssl {true,false}]
                             [-h] [file]

Loads treatment center details into the database

positional arguments:
  file                   application configuration file

named arguments:
  -f FILE, --file FILE   Path to the locator spreadsheet on disk
  -u URL, --url URL      URL to  the  SAMSHA  locator  spreasheet (default:
                         https://findtreatment.samhsa.gov)
  -a ACCESSKEY, --accesskey ACCESSKEY
                         The AWS access key to use. (default: )
  -s SECRETKEY, --secretkey SECRETKEY
                         The AWS secret key to use. (default: )
  -b BUCKET, --bucket BUCKET
                         The AWS bucket to store data into
  -r REGION, --region REGION
                         The AWS region that the bucket lives (default: us-
                         east-1)
  -e ENDPOINT, --endpoint ENDPOINT
                         The AWS API endpoint (default: )
  --host HOST            The IP address or hostname  of the POSTGRES server
                         (defaults to localhost)
  -p PORT, --port PORT   The port number of  the  POSTGRES server (defaults
                         to 5432) (default: 5432)
  -d DATABASE, --database DATABASE
                         The postgres  database  to  store  the  data  into
                         (default 0) (default: postgres)
  -U USERNAME, --username USERNAME
                         (default: postgres)
  -P PASSWORD, --password PASSWORD
                         (default: )
  -useIAM {true,false}   Set  this  flag  to   control   what  to  use  for
                         authenticating to the  database backend. (default:
                         false)
  -skipS3 {true,false}   (default: false)
  -ssl {true,false}      (default: true)
  -h, --help             show this help message and exit
```

The data collection command will store backups of the spreadsheet in S3 for debugging purposes if a processing error occurs.


## RDS
Instructions for configuring RDS with IAM DB authentication can be found [here](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.Enabling.html).


### Example IAM Policy
```json
{
   "Version": "2012-10-17",
   "Statement": [
      {
         "Effect": "Allow",
         "Action": [
             "rds-db:connect"
         ],
         "Resource": [
             "arn:aws:rds-db:us-east-2:1234567890:dbuser:db-ABCDEFGHIJKL01234/db_user"
         ]
      }
   ]
}
```

### Installing extensions
Detailed instructions on configuring RDS with PostGIs can be found [here](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Appendix.PostgreSQL.CommonDBATasks.html#Appendix.PostgreSQL.CommonDBATasks.PostGIS).

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS intarray;
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

### Granting connection permissions
```sql
CREATE USER your_database_user WITH LOGIN;
GRANT USAGE ON SCHEMA public TO your_database_user;
```

### Granting access to the GIS schemas
```sql
GRANT USAGE ON SCHEMA topology TO your_database_user;
GRANT USAGE ON SCHEMA tiger TO your_database_user;
GRANT USAGE ON SCHEMA tiger_data TO your_database_user;
GRANT SELECT ON ALL TABLES IN SCHEMA topology TO your_database_user;
GRANT SELECT ON ALL TABLES IN SCHEMA tiger TO your_database_user;
GRANT SELECT ON ALL TABLES IN SCHEMA tiger_data TO your_database_user;
```
