Ethercis Composition Service
=============================

This module implements the logic associated to queries dealing with Compositions.

The service is technically a simple wrapper to actual method invocation of DAO instances (see ehrdao for more details).

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

2015/12/23

- Update of a composition with format RAW XML (e.g. canonical XML) is not (well) supported. It does a complete replacement
of the original care entry.
