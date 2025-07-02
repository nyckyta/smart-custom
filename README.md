# smart-custom

## Requerements, questions

1. We want to create entities with custom(virtual) set of fields in runtime, without redeploy and ideally even restart.
  - Entities should support basic types (integer (64bit), string, boolean (check), floating)
  - Entities should support references to other entities, either static or dynamic
  - Entities should have rules and conditions applied to fields e.g. max number of numberical types, or max length for strings
  - Entities should support smart-like functionality
    - Validation by entitiy specific permissions for CRUD operations
    - Access to the entity component by id/criteria/via openAPI


2. Each service should be able to create such entities for itself ?
 - Either it is generalized service from which other entities depend on
 - Or it is a static library each service uses locally

3. Entities migration?
 - What if entities to which current entity is bound will migrate to other databases or will be removed after migration?


## Static tables/dynamic work (per service / ultimate service)

1. Design
 - We create one table that will be registry for all other virtual tables.
 - Second table will have list of colums with references to the first table. (or mby store list of columus as embedded structure inside a table would be better?)
 - Third table will have actual data values. 
 - One value is reference on parent table, second value is ID and last value is reference. 
 - Adding new table will be the same as creating a row inside an existing table
 - Removing new table will be the same as deleting a row
 - updating new table can be done only via updating set of columns
 - list of fields will be specified at creation time
 - editing list of fields will be the same as upgrading a complex structured field
 - we can read elements for the row using indexes
 2. How to control data evolution?
 - We may have a version on the table entity
  - Needed backward compatibility, old records should not be mandatory to upgrade or enforce data upgrade via API
  - Removal of mandatory/non mandatory entities simply ignore the field 
 3. Issues
 - Remote service requires a lot of work. We absolutely need ability to create references to other entities. If other entities schema is changed in anyway, we need a reliable approach to know this. Either it will be polling mechanism or ddl upgrade sent to the service. Seems
 as a lot of work for a small gain. One pros, is that we can have entities that depend on entities for other services, but is this really good idea? In theory it allows to create a huge number of unnecessary dependencies between services, it can lead to absolute disaster and circual dependencies between services, *bad pattern!!!*
 - read/write/schema validation will require executing, need to find the way avoiding circular dependecies
 
## dynamic tables (per service)
1. Design
 - each virtual db will represent schema in the database
 - small lib is reponsible for creating and managing schema as well as providing info about existing tables
 - each table represent actual table in the db (separate schema, so no overlaps with liquibase)
 - library provides interfaces for getting information about existing tables
 - permissions for creating tables are controlled on the side of the app (see/manage/delete tables) (sounds very tricky need to be carefull, mby, think more)
 - what about permissions per table? it is statically linked now. We can send them dynamically to account server
 - we can have a single endpoint where we dynamically receive set of fields together with id of the table
 - From id table we derive permissions and check them
 - if permissions are ok, we match provided fields with the request and upsert/update/delete entitiy
 - if entity receives new field (force specify default when mandatory, otherwise null)
 - if entity loses field, we simply remove it from schema
 - we track changes on custom fields (create/updated/deleted)
 - we support references by explicitly setting fields with references (static tables supported must be specified explicitly or mby we find the way of specifying them dynamically)
 
2. Data evolution
 - pretty the same as static tables with some conditions

3. Issues
 - security, permissions may be hard to handle
 - deriving entity from the request may also be tricky from security perspective

-- Custom class loaders.


## Features
### Table creation
-- Validate format of the field names and table name: done
-- Support tables drop : done
-- CRUD operations to tables
-- Error handling avoiding exceptions
  - - basic checks: done
  - - Wrap sql exceptions (ideally vendor independant)
  - - error codes ?
-- String fields
  - - Support default values for columns : done 
  - - Support null/non null values : done
  - - Support min length/max length conditions: done
  - - Support regex validation
-- integer fields
  - - Support default values for columns: done
  - - Support null/non null values: done
  - - Support min/max values: done
-- boolean fields
  - - Support default values for columns: done
  - - Support null/non null values: done
-- floating fields
  - - Support default values for columns: done
  - - Support null/non null values: done
  - - Support min/max values: done
-- Security
  - - SQL Injection protection. escape all special symbols before concat to string : done
  - - Check tables live in dedicated schema, organize separation of roles (dedicated user for custom schema?
-- References
  - - Support references between custom entities
-- API for metainformation about entities
  - - Get list of all custom entities
  - - Get list of fields for custom entity
-- What should we do with transactions?

### Read/Write operation

