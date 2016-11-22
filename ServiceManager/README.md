Ethercis ServiceManager
========================

The ServiceManager is the underlying layer used by all services deployed on a server instance. Essentially, it contains common classes used to declare and manage services:

- Service container (by a run-level manager into an instance wide service registry)
- Sessions and their properties
- Service parameters (generally declared in a `services.properties` file)
- Basic security hooks
- Exceptions
- JMX registration
- Runtime singleton (RunTimeSingleton single instance) used to convey common properties and other data to all services running in a container. The singleton points to the unique service registry used by services to invoke methods in running instances 

Services can be implemented simply by means of annotations, specialization and simple method invocations:

    @Service(id ="MyService", version="1.0", system=true)
    
    @RunLevelActions(value = {
            @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
            @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP") })
    
    public class MyService extends ServiceDataCluster implements I_MyService, MyServiceMBean {

    final private String ME = "MyService";
    final private String Version = "1.0";
    
    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)throws ServiceManagerException {
        super.doInit(global, serviceInfo);
        
        // do some service specific initialization

        //get a resource service instance
        putObject(I_Info.JMX_PREFIX + ME, this);
        
        log.info("MyService service started...");
    }



- `doInit` is invoked by the RunLevel manager after the service is instantiated.
- `putObject` register the service with JMX (please note the public for JMX should be defined in the corresponding
interface (`xyzMBean`) or by a JMX annotation



Credit
------

ServiceManager is based on XmlBlaster ([http://xmlbalster.org](http://xmlbalster.org)). It is originally a Message Oriented Middleware, with a very interesting service (or plugin) framework allowing to perform all sorts of interaction at run-time using, but not limited to, JMX. It comes also with a convenient way to manage sessions and authorizations.

How To Compile The Module
-------------------------
REQUIREMENTS

- Java 1.8 or higher
- Maven 3.3 or higher

INSTALLATION

The compilation and artifact generation is performed by `mvn clean install`.

Dependencies
------------
Dependencies that are not resolved by Maven should be located in a local repository. An archive containing these local dependencies is provided on this site:

- ehrxml : XmlBeans compiled resources/schemas (definition, service mapper etc.)
- openEHR.v1.OperationalTemplate -- XmlBeans compilation of schema Template.xsd (see in main/resources/schemas)
- openEHR.v1.Template -- XmlBeans compilation of schema CompositionTemplate.xsd (see in main/resources/schemas)
- adl-parser -- openehr java ref library archetype parser

Tests
-----

Tests are disabled in POM.XML.

Known issues
============

2015/12/23

- it is expected to link the authorization mechanism with Shiro. At the moment, authorization is not checked.
- There is no inter-service communications. It has been removed from the XmlBlaster original implementation since it is not required for this type of deployment.
