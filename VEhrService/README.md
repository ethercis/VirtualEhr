Ethercis vEhr Service
=====================

This module implements the entry point to all service queries. It consists of:

- A launcher to start the server instance (Launcher) with an embedded protocol stack (HTTP for now)
- A servlet acting as a services container (VEhrGateServlet).
- A number of specialized system services:
. Query handler service (AccessGateService)
.  Parser service to adapt various syntax to ethercis method invocation (EhrScapeURIParser here)
. Query dispatcher service to direct queries to a matching service (RequestDispatcher)
- A set of formatter to assemble query response depending on the content type (XML, JSON etc.)

Internally, the query syntax is following the Operation Centric Pattern (as opposed to Resource Centric) similarly
to SOAP, XML-RPC for example. This pattern allows fine grained access control since authorization by 'filtered targets' 
is easily implemented.

HTTP server is using embedded Jetty.

How To Compile The Module
-------------------------
REQUIREMENTS

- Java 1.8 or higher
- Maven 3.3 or higher

INSTALLATION

The compilation and artifact generation is performed by `mvn clean install`.

Starting the server instance
----------------------------

See the launch script in section deployment on this site for more details

The following application parameters are required:
 
- server_port: port number to bind the http server to (default: 8080)
- server_host: hostname to bind the http server to (an IP address or hostname linked to a network interface)
- propertyFile: path to the services property file (see section deployment on this site for more details)
- java_util_logging_config_file: configuration of specific Java Logs (still used for auditing)
- dialect: specify the REST query syntax to use (should be set to EHRSCAPE)
- debug (optional) switch the logs in DEBUG (can also be set via JMX)

Example:

        launcher.start(new String[]{
                "-propertyFile", "resources/services.properties",
                "-java_util_logging_config_file", "resources/logging.properties",
                "-servicesFile", "resources/services.xml",
                "-dialect", "EHRSCAPE",
                "-server_port", "8080",
                "-server_host", "localhost",
                "-debug", "true"
        });

NB. Logging relies mostly on log4j. Logging configuration is set in a property file declared with a JVM parameter:

`-Dlog4j.configuration=file:${RUNTIME_ETC}/log4j.xml`
`-Dlog4j.configuration=file:C:/Development/eCIS/VirtualEhr/EhrService/resources/log4j.xml`

Similarly, Java Logging configuration can be given with a JVM parameter:

`-Djava.util.logging.config.file=${RUNTIME_ETC}/logging.properties`

Enabling JMX on the server
--------------------------

The JVM can be set to allow JMX queries (unsecure example follows, don't use it in production):

	-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.port=8999 \
    -Dcom.sun.management.jmxremote.local.only=false \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	
See [jmx remote](http://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html "jmx remote") for more details on this.

To monitor JMX activities, you can use JMC: [Java Mission Control](http://www.oracle.com/technetwork/java/javaseproducts/mission-control/java-mission-control-1998576.html "Java Mission Control")


Dependencies
------------
See pom.xml, this module does not rely on 'exotic' dependencies.

Tests
-----

Tests are disabled in POM.XML.

Known issues
============

2015/12/23

- CORS is implemented but needs more testing
- All queries are now processed synchronously. Asynchronous handling is implemented however, but needs to be bound to
actual REST queries if required.
