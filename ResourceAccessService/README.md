Ethercis Resource Access Service
================================

This module deals with loading and configuration of components used to access the application resources including:

- Database with its corresponding dialect
- Knowledge Service

The idea is to extend this service to any relevant resource system associated to a server instance (terminology,
imaging, ...)

Please note most actions and statistics are provided via JMX


How To Compile The Module
-------------------------
REQUIREMENTS

- Java 1.8 or higher
- Maven 3.3 or higher

INSTALLATION

The compilation and artifact generation is performed by `mvn clean install`.

Runtime Parameters
------------------

- `server.persistence.implementation` the persistence layer used. Should be set to `jooq`
- `server.persistence.jooq.dialect` should be set to POSTGRES
- `server.persistence.jooq.url` the JDBC connection string, example: `jdbc:postgresql://localhost:5432/ethercis`
- `server.persistence.jooq.login` login name to connect to the DB
- `server.persistence.jooq.password` password to connect to the DB

Dependencies
------------
See pom.xml, this module does not rely on 'exotic' dependencies.

Tests
-----

Tests are disabled in POM.XML.

Known issues
============

