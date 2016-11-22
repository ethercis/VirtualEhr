Ethercis Party Identifier Service
=================================

This module implements the logic associated to queries dealing with Identified Parties. 

NB. it is not really used at the moment as a service since Party management is site dependent. It can be associated for example to an existing internal DB, an MPI, an LDAP directory, Active Directory etc.

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

- Needs extension to operate in a production environment, if applicable
