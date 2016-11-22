Ethercis Logon Service and Security Manager Service
===================================================

This module implements two services described below.

Logon Service
-------------

This service manages the session activities for a user: connection/disconnection to the platform.

The service is a facade of the session manager. It allows a number of session parameters to be passed to the service
from the HTTP header:

- SECRET\_SESSION_ID: `Ehr-Session` (optional) force a secret session id for this session, or try to reconnect to an existing ssid
- SESSION_NAME: `x-session-name`(optional) force a public session name. If it use for reconnect, check if the reconnect is legitimate (same user and check the credentials)
- CLUSTER_NODE: `x-cluster_node` (optional) boolean, indicate this is a connection from a cluster node
- REFRESH_SESSION: `x-refresh-session` (optional) boolean, indicate that this session is to be refreshed (push timeout back)
- RECONNECT: `x-reconnect` (optional) boolean, true if this is a reconnect
- SESSION_TIMEOUT: `x-session-timeout` (optional) long, sets a timeout for session (default is no timeout)
- MAX_SESSION: `x-max-session` (optional) sets the max number of allowed session for user (1 by default)
- CLEAR_SESSION: `x-clear-session` (optional) clear all sessions for this users if true
- BYPASS_CREDENTIAL: `x-bypass-credential` (optional) no credential check for this user (security hazard here!!!)
- CLIENT_IP: `x-client-ip` (optional) address of this user 

Upon successful connection, the service returns in the HTTP header:

- SECRET\_SESSION\_ID: `Ehr-Session` secret session id for this session (use this for further transactions)
- SESSION_NAME: `x-session-name` public session name.
- RECONNECT: `x-reconnect` boolean, true if this is a reconnect
- SESSIONS_IN_USE: `x-sessions-in-use number` of sessions used by user
- CLIENTPROPERTY\_RCVTIMESTAMPSTR: `__rcvTimestampStr` timestamp of session creation

**Runtime Parameters**


Some tweaking of session parameter defaults is possible:

- `session.timeout` specifies the default timeout in ms (ex: 1800000 for 30'), default is one day
- `session.maxSessions` max number of concurrent sessions (default is 10)
- `session.clearSessions` clear all current sessions for a user when connecting (default is false)
- `session.reconnectSameClientOnly` true if only the same client with session id can reconnect to a current session
- `session.secretSessionId` force a session id (for TEST purpose only) 

ServiceSecurityManager
----------------------

The service load and initialize the underlying security policy provider (here SHIRO). 


**Runtime Parameter**

- `server.security.policy.type` policy provider, default is SHIRO
- `server.security.shiro.inipath` SHIRO simple authentication profiles (INI format)


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

- Currently, the authentication policy is basic. It should be extended to production scenario (linked to an LDAP directory in particular)
