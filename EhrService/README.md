Ethercis Ehr Service
====================

This module implements the logic associated to queries dealing with Ehr and Ehr Status.

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

`ehr.subject.mode` either `EXTERNALREF` or `IDENTIFIER` (default is EXTERNALREF)

- if the mode is EXTERNALREF, the subject is identified by a simple code/namespace pair (usual case)
- if the mode is IDENTIFIER, the subject is identified with an identifier. This is useful when a subject can have 
multiple identifiers provided by different systems

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
