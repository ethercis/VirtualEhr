Ethercis Knowledge Service
==========================

This module implements the logic associated to queries dealing with the Cache Knowledge. In particular it allows
interactions with utilities from the HTTP interface:

- retrieve the list of currently loaded templates
- upload an operational template

Please note most actions and statistics are provided via JMX

The service is technically a simple wrapper to actual method invocation of DAO instances (see ehrdao for more details).

How To Compile The Module
-------------------------
REQUIREMENTS

- Java 1.8 or higher
- Maven 3.3 or higher

INSTALLATION

The compilation and artifact generation is performed by `mvn clean install`.

Runtime Parameters
------------------

Knowledge cache specific parameters are discussed in ehrdao.

Dependencies
------------
See pom.xml, this module does not rely on 'exotic' dependencies.

Tests
-----

Tests are disabled in POM.XML.

Known issues
============

2015/12/23

- No 'dump template' query is currently implemented. 
