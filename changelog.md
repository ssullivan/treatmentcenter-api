---
Changelog
--
v1.10.4:
* BUG: Fixed a bug in scoring by gender (also added some support for variability in the input)

v1.10.3:
* BUG: Fixed a bug in the documentation for the militaryImp (importance) field
* MISC: Added some checks to make sure that the service codes sets are of reasonable length (<200)
* BUG: Fix regression for handling multiple service code sets
 
v1.10.2:
* BUG: We were not validating the size of the conditions. to prevent too many coming in we are going to set a limit of 200

v1.10.1:
* BUG: Fixed a bug in the swagger docs for trauma types

v1.10.0:
* Added new API inputs to control how the facility scoring works

v1.9.0:
* Initial version of composite score. API still needs to be updated to provide the module inputs

v1.8.3:
* Made the join operation for multiple search sets configurable 

v1.8.2
* FEATURE: Allow the specification of multiple search sets to search by. This is achieved by doing something like serviceCodes=a,b,c&serviceCodes=d,e,f&matchAny=x,y,z

v1.8.1
* FEATURE: Added the ability to specify service as comma separated lists (this only affects the v2)

v1.8.0
* FEATURE: Added a v2 search endpoint that can take a list of services to match any on
* BUG: Fixed a few internal bugs related to mapping geo units

v1.7.0
* FEATURE: Added a new field called 'available' to the Facility results. This maintains
  the relationship between categories, and services 

v1.6.1
* BUG: Fixed a bug where Redis Client was throwing errors when the must not list was empty
* BUG: Fixed a bug where we failed to restore auto flush commands which caused the request to time out

v1.6.0
* FEATURE: Added a new option to search API to match any of the requested service codes. This is disabled by default. matchAny=true must be set to enable this.
* BUG: Fixed a bad recursive call that introduced in v1.5.0

v1.5.0
* FEATURE: Added support for negating serviceCodes by prefixing with a !

v1.4.0
* FEATURE: Added support for searching by postalCode

v1.3.0
* FEATURE: Added support for CORS (Allowed Origins can be configured via ENV VAR CORS_ALLOWED_ORIGINS)

v1.2.1
* BUG: Fixed a bug where we were returning results outside of the specified geo radius
