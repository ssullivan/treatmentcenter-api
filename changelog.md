---
Changelog

v1.6.0-SNAPSHOT
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