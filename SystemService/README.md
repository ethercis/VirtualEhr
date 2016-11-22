Ethercis System Service
=======================
The service is technically a simple wrapper to actual method invocation of DAO instances (see ehrdao for more details).

It does not support any query capability since systems should be managed by another application.

How To Compile The Module
-------------------------
REQUIREMENTS

- Java 1.8 or higher
- Maven 3.3 or higher

INSTALLATION

The compilation and artifact generation is performed by `mvn clean install`.

Dependencies
------------
See pom.xml, this module does not rely on 'exotic' dependencies.

Tests
-----

Tests are disabled in POM.XML.

Known issues
============


