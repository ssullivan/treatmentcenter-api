# treatmentcenter-api OpenAPI Specification
[![Build Status](https://travis-ci.org/ssullivan/treatmentcenter-api.svg?branch=master)](https://travis-ci.org/ssullivan/treatmentcenter-api)
[![codecov](https://codecov.io/gh/ssullivan/treatmentcenter-api/branch/master/graph/badge.svg)](https://codecov.io/gh/ssullivan/treatmentcenter-api)

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

### Loading data into Redis/ElastiCache
java -jar application.jar load-treatment-centers -f /path/json.gz --host localhost --port 6379

### Configuration of Container

#### Loading Data into Redis/ElastiCache

Initial deployment involved deploying a Redis backup. Work is in-progress to create an automated flow
for updating the backend.


#### Environment Variables
| Env | Default | Description |
| --- | ------- | ----------- |
| CORS_ALLOWED_ORIGINS | https?://.*.centerlocator.org | Controls what domains are allowed to hit the service |
| ENVIRONMENT | - | Controls what domain is used in the swagger docs |
| POSTALCODES_US_PATH | /treatmentcenter-api-latest/data/US.txt | A list of lat/lon for postcal codes | 


### Redis Key Structure

| PREFIX | Type | Description |
| ------ | ---- | ----------- |
| index:facility_by_service:`{service}` | set | Stores facility ids associated with `{service}` |
| index:facility_by_category:`{category}` | set | Stores facility ids associated with `{category}` |
| index:facility_by_geo | geoset | Stores the facilities by lat,lon |
| treatment:facilities:`{id}`  | hmap | Stores the key/values for the facility. `id` is a long |
| search:counter | incr | Several of the searches rely on zinterstore methods. This is used to provide a uniq id to each request |

### Loading Data
This is process is still fairly manual and we are in the process of automating it. Sample data can be
found in the /data folder in this repo.

* Fetch spreadsheet from SAMSHA 
* Convert spreadsheet into two JSON/JSONL files -> (`service_codes_records.json`, `facilities_geocoded.json.gz`)
* Load service_code_records JSON using the Dropwizard task `LoadCategoriesAndServices`
* Load facilities JSONL using the Dropwizard task `LoadTreatmentFacilities

### Querying with the Rest API


 