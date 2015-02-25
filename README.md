#org.brutusin:jsonsrv [![Build Status](https://api.travis-ci.org/brutusin/jsonsrv.svg?branch=master)](https://travis-ci.org/brutusin/jsonsrv) [![Maven Central Latest Version](https://maven-badges.herokuapp.com/maven-central/org.brutusin/jsonsrv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.brutusin/jsonsrv/)
A java web framework for easily exposing business methods as self-descriptive JSON web services over HTTP. 

Motivated by the creation of Javascript/AJAX/JSON web interfaces, the goal of this library is to allow a very simple and efficient implementation of java web services, guaranteeing a correct usage of the HTTP semantics and also to constitute a self-describing repository of services.

**Examples** 
* Service execution:
  * Request: `http://localhost:8080/jsonsrv?id=date`
  * Response: `{"value":"2015-01-28T16:04:25.906+01:00"}`
* Service listing:
  * Request: `http://localhost:8080/jsonsrv`
  * Response: `{"value":["exception","date","hello","version"]}`
* Service output JSON-schema:
  * Request: `http://localhost:8080/jsonsrv?id=date&schema=o`
  * Response: `{"type":"string"}`

**Main features**
* **Self-description**: Based on [JSON Schema](http://json-schema.org/). Input/output schemas of the service can be obtained using the `schema` url-parameter. This feature enables automatic form generation for testing, and enhances service readability, usability and maintainability.
* **Complex input/output data**: Given that input schema is known, input data can have an arbitrary complexity.
* **Handles HTTP transparently**: Caching and status codes are handled automatically. Service code is only related to the business. Neither HTTP nor serialization related coding.
* **Easy implementation**: Business is coded as simple `O execute(I input)` methods . No annotations needed.
* **Plugable rendering**: [Custom renderers](#custom-renderers) can be developed in order to provide more advanced visualizations. 
* **Optional Spring integration**: Enhancing integration and allowing to take advantage of [IoC](http://en.wikipedia.org/wiki/Inversion_of_control) for implementing loosely-coupled maintainable services.


**Table of Contents**

- [org.brutusin:jsonsrv](#orgbrutusinjsonsrv-)
  - [Definitions](#definitions)
  - [Usage](#usage)
    - [Maven dependency](#maven-dependency)
    - [Service implementation](#service-implementation)
      - [SafeAction](#safeaction)
      - [UnsafeAction](#unsafeaction)
    - [Service registration](#service-registration)
      - [JsonServlet](#jsonservlet)
      - [SpringJsonServlet](#springjsonservlet)
    - [Running](#running)
  - [Action life-cycle](#action-life-cycle)
  - [Implementation details](#implementation-details)
    - [JSON SPI](#json-spi)
    - [Threading issues](#threading-issues)
    - [Response object and error handling](#response-object-and-error-handling)
    - [HTTP response](#http-response)
      - [Status codes](#status-codes)
      - [Content-Type header](#content-type-header)
      - [Caching](#caching)
      - [Getting servlet objects from actions](#getting-servlet-objects-from-actions)
  - [Configuration and extensions](#configuration-and-extensions)
    - [Custom renderers](#custom-renderers)
    - [Servlets init params](#servlets-init-params)
    - [Servlets overridable methods](#servlets-overridable-methods)
  - [Example](#example)
  - [Main stack](#main-stack)
  - [Brutusin dependent modules](#brutusin-dependent-modules)
  - [Support, bugs and requests](#support-bugs-and-requests)
  - [Authors](#authors)
  - [License](#license)

##Definitions
The following concepts are used throughout the rest of the documentation, and must be defined for a better understanding:
* **Action**: An action is a class extending [JsonAction](src/main/java/org/brutusin/jsonsrv/JsonAction.java). Sometimes it is used to refer to a concrete instance.
* **Service**: A service is an exposed action instance (published by the framework), bound (also "mapped") to a unique identifier `id`;

##Usage
###Maven dependency 
This library is meant to be used by a java web module. If you are using maven, add this dependency to your war `pom.xml`:
```xml
<dependency>
    <groupId>org.brutusin</groupId>
    <artifactId>jsonsrv</artifactId>
</dependency>
```
Click [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.brutusin%22%20a%3A%22jsonsrv%22) to see the latest available version released to the Maven Central Repository.

If you are not using maven and need help you can ask [here](https://github.com/brutusin/jsonsrv/issues).

###Service implementation
Business is coded in custom classes extending either from [SafeAction](src/main/java/org/brutusin/jsonsrv/SafeAction.java), or [UnsafeAction](src/main/java/org/brutusin/jsonsrv/UnsafeAction.java), and using POJOs to define input/output parameters. 

According to [rfc7231](http://www.rfc-editor.org/rfc/rfc7231.txt) section 4.2.1:
> ... Request methods are considered "safe" if their defined semantics are
   essentially read-only; i.e., the client does not request, and does
   not expect, any state change on the origin server as a result of
   applying a safe method to a target resource.  Likewise, reasonable
   use of a safe method is not expected to cause any harm, loss of
   property, or unusual burden on the origin server...

####SafeAction
[SafeAction](src/main/java/org/brutusin/jsonsrv/SafeAction.java) is used to implement *safe* business logic, that is, logic that has no side-effects expected by the user. Results of these actions are [cacheable](#caching), and both `GET` and `POST` request methods are allowed.

Example:
```java
public class HelloWorldAction extends SafeAction<String, String> {
    @Override
    public CachingInfo getCachingInfo(String input) {
        return ExpiringCachingInfo.ONE_DAY;
    }
    
    @Override
    public String execute(String input) throws Exception {
        return "Hello " + input + "!";
    }
}
```

####UnsafeAction
On the other side, [UnsafeAction](src/main/java/org/brutusin/jsonsrv/UnsafeAction.java) is used to implement *unsafe* business logic, that has side-effects expected by the user, like for example, a state change in a business model.

Results of these actions are not cacheable, and only the `POST` request method is allowed.

Example:
```java
public class CheckoutAction extends UnsafeAction<Void, Void> {
    @Override
    public void execute() throws Exception {
        // get shopping cart from HttpSession
        // start transaction
        // update stock
        // perform payment
        // end transaction
    }
}
```

###Service registration
Two alternative framework servlets are available, covering two different configuration scenarios: 
* [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java): Base servlet that loads service definitions from `jsonsrv.json` (explained later). No dependency injection supported.
* [SpringJsonServlet](src/main/java/org/brutusin/jsonsrv/SpringJsonServlet.java): Extending the previous servlet, this servlet loads the service definitions from [Spring](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/xsd-config.html) configuration XML files (by default `jsonsrv.xml`). Spring dependencies have a `<scope>provided</scope>` in this module, so in order to use this servlet, [spring-context](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.springframework%22%20a%3A%22spring-context%22) artifacts must be provided by the client module at runtime. 

####JsonServlet
**Web module configuration**

In the `web.xml` configure the following mapping for this framework servlet:

```xml
...
<servlet>
    <servlet-name>json-servlet</servlet-name>
    <servlet-class>org.brutusin.jsonsrv.JsonServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>json-servlet</servlet-name>
    <url-pattern>/srv</url-pattern>
</servlet-mapping>
...
```
This way, all requests under the `/srv` path will be processed by it.

**Service registration**

Register the actions in order to the framework can find them, by creating a `jsonsrv.json` file in the root namespace (so it can be loaded by `getClassLoader().getResources("jsonsrv.json")`).

Example:
```json
[
  {
    "id": "hello",
    "className": "org.brutusin.jsonsrv.example.complex.HelloWorldAction"
  },
  {
    "id": "date",
    "className": "org.brutusin.jsonsrv.example.GetDateAction"
  }
]
```

####SpringJsonServlet
**Web module configuration**

In the `web.xml` configure the following mapping for this framework servlet:

```xml
...
<servlet>
    <servlet-name>json-servlet</servlet-name>
    <servlet-class>org.brutusin.jsonsrv.SpringJsonServlet</servlet-class>
    <init-param>
        <!-- Optional path to an aditional cfg file. See "Servlets init-params" section-->
        <param-name>spring-cfg</param-name>
        <param-value>/application-context.xml</param-value>    
    </init-param>
</servlet>
<servlet-mapping>
    <servlet-name>json-servlet</servlet-name>
    <url-pattern>/srv</url-pattern>
</servlet-mapping>
...
```

**Service registration**

Register the actions in order to the framework can find them, by creating a `jsonsrv.xml` file in the root namespace (so it can be loaded by `getClassLoader().getResources("jsonsrv.xml")`).

Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans 
	   					   http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="date" class="org.brutusin.jsonsrv.example.spring.GetDateAction">
        <property name="datePattern" value="yyyy-MM-dd'T'HH:mm:ss.SSSXXX"/>
    </bean>
    <bean id="time" class="org.brutusin.jsonsrv.example.spring.GetDateAction">
        <property name="datePattern" value="h:mm a"/>
    </bean>
</beans>
```
The framework will automatically find all beans of the spring context that are instances of `JsonAction`, and will use their `id` property as id for the service.

Notice that the same action class can be used by different services, an dependency injection can be used. 

###Running

Run the web application and test it form the web browser. Both POST and GET methods are supported.

**Supported URL parameters**

URL parameter  | Description
------------- | -------------
**`id`** | Id of the service to execute, as registered in the configuration file
**`input`** | json representation of the input
**`schema`** | Set it to `i` or `o` to return the schema of the input or output of the service respectively

**Use cases**

Case | URL  | Sample response payload
------| ------- | ---------
Service listing | `srv` | `{"value":["date","exception","hello","version"]}`
Service execution | `srv?id=example&input=%22world%22` | `{"value":"Hello world!"}`
Service input schema | `srv?id=example&schema=i` | `{"type":"string"}`
Service output schema | `srv?id=example&schema=o` | `{"type":"object","properties":{"error":{"type":"object","properties":{"code":{"type":"integer","required":true},"data":{"type":"any"},"meaning":{"type":"string","required":true},"message":{"type":"string","required":true}}},"value":{"type":"string"}}}`

## Action life-cycle
On servlet initialization, the service mappings are loaded from the configuration file(s), and for each mapping, an instance of the action is created and bound to the service id.

This action will serve all the requests to the service, so [thread-safety issues](#threading-issues) should be considered.

On request processing the following methods are executed: 

- For [SafeActions](src/main/java/org/brutusin/jsonsrv/SafeAction.java):
  1. `getCachingInfo(I input)`: That returns caching information for this request.
  2. `execute(I input)`: Depending on the client request being conditional, and on the value returned by the previous method, this method is or is not executed. (see [caching section](#caching) for more details).
- For [UnafeActions](src/main/java/org/brutusin/jsonsrv/SafeAction.java):
 1. `execute(I input)`
 
## Implementation details
###JSON SPI
This module makes use of the [JSON SPI](https://github.com/brutusin/commons/tree/master/src/main/java/org/brutusin/commons/json/spi), so a JSON service provider like [json-codec-jackson](https://github.com/brutusin/json-codec-jackson) is needed at runtime. The choosen provider will determine JSON serialization, validation, parsing and schema generation.

###Threading issues
The framework creates a single action instance per service (action mapping in `jsonsrv.json`) to serve all requests, that is, actions will run on a multithreaded environment, so be aware that they must handle concurrent requests and be careful to synchronize access to shared resources.

###Response object and error handling
All HTTP requests processed by the framework return a JSON payload meeting the following schema:
```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "object",
      "properties": {
        "code": {
          "type": "integer",
          "required": true
        },
        "meaning": {
          "type": "string",
          "required": true
        },
        "message": {
          "type": "string",
          "required": true
        },        
		"data": {
          "type": "any"
        }
      }
    },
    "value": {
      "type": "any"
    }
  }
}
```
being the `value` property schema dependent on the action queried (in fact, being the output schema of the service). 

The following error codes are defined, aligned with the [JSON_RPC 2.0 specification](http://www.jsonrpc.org/specification#error_object):

Code | Meaning  | Description
------| ------- | ---------
-32700|Parse error|Invalid JSON was received by the server. An error occurred on the server while parsing the JSON input
-32601|Service not found|The service does not exist / is not available
-32602|Invalid input|Invalid service input. Returned when received input does not meet schema restrictions and when action `execute(..)` method throws an `IllegalArgumentException` 
-32000|Security error| In case of a `SecurityException` thrown by action method `execute(..)`
-32001|Application error|Error contemplated by the application logic. In case of a checked exception thrown by action method `execute(..)`
-32002|HTTP invalid method|The request method is not allowed by target resource. In case of a GET request to an unsafe action.
-32603|Internal error|In an internal error occurs or action method `execute(..)` throws an unchecked (runtime) exception.

###HTTP response
Although HTTP responses can be decorated using [custom renderers](#custom-renderers), the default behaviour is as follows:
####Status codes
Depending on the returned JSON payload, the following status codes are returned:

[HTTP response status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)| Case
------| ------- |
200/304 | if `error` is `null` or `error.code` equals `-32001`  (see [Caching section](#caching) for more details)
400 | if `error.code` equals `-32700` or `-32602`
403 | if `error.code` equals `-32000`)
404 | if `error.code` equals `-32601`)
405 | if `error.code` equals `-32002`)
500 | any other error

####Content-Type header
`Content-Type:application/json`

####Caching
The framework automatically handles caching depending on these factors: 
* Action being *safe*.
* Implementation of the `public CachingInfo getCachingInfo(I input)` method of the action (by default returns `null`, meaning no caching).
* Execution with/without errors.
* Conditional request header `If-None-Match` present.

**Caching algorithm**: The following algorithm determines action execution and HTTP response contents:
* If action is instance of [SafeAction](src/main/java/org/brutusin/jsonsrv/SafeAction.java)
* Call `getCachingInfo(I input)` and get the [CachingInfo](src/main/java/org/brutusin/jsonsrv/caching) instance for the current request.
* Perform the conditional execution of the action, that is:
  *  If the request is conditional (cointains an etag, i.e. `If-None-Match` HTTP header) and `CachingInfo` is an instance of [ConditionalCachingInfo](src/main/java/org/brutusin/jsonsrv/caching/ConditionalCachingInfo.java) and `ConditionalCachingInfo.getEtag()` matches the received etag, then: Skip the action execution, set response status code to `304 (NOT MODIFIED)` and mark the execution to return no payload after headers processing.
  *  Else: Execute the action: `execute(I input)`.
* If an error occurred (except `-32000`) or execution `CachingInfo` is `null` then the response is not cacheable and the following HTTP headers are returned:
```
Expires:Thu, 01 Jan 1970 00:00:00 GMT
Cache-Control:max-age=0, no-cache, no-store
Pragma:no-cache
```
* Else if `CachingInfo` is an instance of [ConditionalCachingInfo](src/main/java/org/brutusin/jsonsrv/caching/ConditionalCachingInfo.java): 
```
Expires:Thu, 01 Jan 1970 00:00:00 GMT
Cache-Control: private, must-revalidate
ETag: W/"<etag>"
```
* Else (`CachingInfo` is an instance of [ExpiringCachingInfo](src/main/java/org/brutusin/jsonsrv/caching/ExpiringCachingInfo.java)) return the following unconditional caching HTTP headers:
```
Expires:Thu, 01 Jan 1970 00:00:00 GMT
Cache-Control:max-age=<max-age>, private, must-revalidate
```

**Note on `POST` requests**: When a *POST* request is received, all responses allowing caching additionally contain  a `Content-Location` header pointing to the url of the *GET* version, as explained in ([rfc7231](http://www.rfc-editor.org/rfc/rfc7231.txt) 4.3.3):
> ... POST caching is not widely implemented.  For cases where an origin server wishes the client to be able to cache the result of a POST in a way that can be reused by a later GET, the origin server MAY send a 200 (OK) response containing the result and a Content-Location header field that has the same value as the POST's effective request URI...

**Note on `Expires` header**: An `Expires` header with an outdated value `Thu, 01 Jan 1970 00:00:00 GMT` is returned in every response regardless of the case. This action is performed in order to avoid legacy shared caches (that might ignore the cache-control header) caching the response, since in every case the `private` directive is used. 
> An origin server might wish to use a relatively new HTTP cache control feature, such as the "private" directive, on a network including older caches that do not understand that feature. The origin server will need to combine the new feature with an Expires field whose value is less than or equal to the Date value. This will prevent older caches from improperly caching the response.

See [rfc2616 sec14.9.3](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3) for more details.

### Getting servlet objects from actions
Despite of being business oriented, actions might need to use some servlet-related objects, like request, response, session, application ... 

For this purpose,  the helper class [JsonActionContext](src/main/java/org/brutusin/jsonsrv/JsonActionContext.java) exists. By making use of thread-locality, this class lets the executing action access their current servlet-related objects, by simply calling `JsonActionContext.getInstance()` in a static way.

##Configuration and extensions

### Custom renderers
Once the framework servlet has processed the request and generated a JSON message to be returned, it sets the status code and the content type of the response and delegates the response rendering (writing to the HTTP response body) to a [Renderer](src/main/java/org/brutusin/jsonsrv/plugin/Renderer.java).
The [default renderer](src/main/java/org/brutusin/jsonsrv/impl/DefaultRenderer.java) simply writes the payload to the response writer:
```java
resp.getWriter().print(json); // being resp a HttpServletResponse
``` 
More advanced functionality can be plugged using custom renderers; for example, [jsonsrv-human-renderer](https://github.com/brutusin/jsonsrv-human-renderer) module that adds and extra "human" mode that eases service testing and improves readability, by the automatic creation of HTML forms from the input schema of the service.

In the following sections, it is explained how to configure a custom render. 

### Servlets init-params
The following optional init-params are supported by both [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java), [SpringJsonServlet](src/main/java/org/brutusin/jsonsrv/SpringJsonServlet.java):
* `schema-parameter-disabled`: Accepts a boolean value for disabling schema queries. Default value is `false` (enabled)
* `renderer`: Class name to the custom render to use. If not specified, the default renderer is used 
* `render-param`: Additional parameter to be passed to the custom renderer, accessible via its `getInitParam()` method
* `access-control-allow-origin`: Determines the presence and value of an `Access-Control-Allow-Origin` HTTP response header in order to enable [CORS](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing) (if not specified, the header is not present in the responses).

Additionaly, only for [SpringJsonServlet](src/main/java/org/brutusin/jsonsrv/SpringJsonServlet.java):
* `spring-cfg`: Path to an additional (all `jsonsrv.xml` in classpath are always used) spring configuration file to use,

### Servlets overridable methods
The following  [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java) methods can be overriden:
* `protected ClassLoader getClassLoader()`: Lets specify a different *ClassLoader* for loading the pluggable resources (configuration file, action classes and render class). If not overridden, `JsonServlet.class.getClassLoader()` is returned.
* `protected Map<String, JsonAction> loadActions()`: To change the way actions are loaded.
* `protected List<String> getSupportedInitParams()`: If subclasses add new `init-param` parameters they have to be declared to be used. 

##Example:
A complete example project is available at [jsonsrv-example](https://github.com/brutusin/jsonsrv-example).  

##Main stack
This module could not be possible without:
* Now moved to [json-codec-jackson](https://github.com/brutusin/json-codec-jackson), but key libraries for the project inception:
  * [FasterXML/jackson stack](https://github.com/FasterXML/jackson): The underlying JSON stack.
  * [com.fasterxml.jackson.module:jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema): For java class to JSON schema mapping. 
  * [com.github.fge:json-schema-validator](https://github.com/fge/json-schema-validator): For validation against a JSON schema.
* [Spring IoC](http://projects.spring.io/spring-framework/): Used optionally to load service mappings from Spring

## Brutusin dependent modules
* [org.brutusin:jsonsrv-human-renderer](https://github.com/brutusin/jsonsrv-human-renderer)
* [org.brutusin:jsonsrv-example](https://github.com/brutusin/jsonsrv-example)

## Support, bugs and requests
https://github.com/brutusin/jsonsrv/issues

## Authors

- Ignacio del Valle Alles (<https://github.com/idelvall/>)

Contributions are always welcome and greatly appreciated!

##License
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
