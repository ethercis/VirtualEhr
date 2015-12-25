Ethercis Authenticate Service
=============================

This module implements the logic associated to users credentials.

Although intended to be used as a service, it is instantiated whenever a new session is created for a user then bound to the session to perform further credentials checks including permissions.

At the moment, Authenticate uses [SHIRO](http://shiro.apache.org/) as security framework, but can be adapted to any
other strategy.

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

- Authorization (permission) is not yet bound to shiro
