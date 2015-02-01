#org.brutusin:jsonsrv [![Build Status](https://api.travis-ci.org/brutusin/jsonsrv.svg?branch=master)](https://travis-ci.org/brutusin/jsonsrv)
A self-describing, java web framework for easily exposing business methods as JSON RPC services over HTTP. 

Motivated by the creation of Javascript/AJAX/JSON web interfaces, the goal of this library is to allow a very simple and efficiently implementation of java services, and being able to execute them by HTTP POST and GET requests, and equally important, constitute a self-describing repository of services.

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
* **Self-describing**: Based on [JSON Schema](http://json-schema.org/). Input/output schemas of the service can be obtained using the `schema` url-parameter. This feature enables automatic form generation for testing, and enhances service readability, usability and maintainability.
* **Complex input/output data**: Given that input schema is known, input data can have an arbitrary complexity.
* **Implicit HTTP semantics**: Caching and status codes are handled automatically. Service code is only related to the business. Neither HTTP nor serialization related coding.
* **Easy implementation**: Business is coded as simple `O execute(I input)` methods . No annotations needed.
* **Plugable rendering**: [Custom renderers](#custom-renderers) can be developed in order to provide more advanced visualizations. 
* **Optional Spring integration**: Enhancing integration and allowing to take advantage of [IoC](http://en.wikipedia.org/wiki/Inversion_of_control) for implementing loosely-coupled maintainable services.


**Table of Contents**

- [org.brutusin:jsonsrv](#orgbrutusinjsonsrv-)
  - [Definitions](#definitions)
  - [Usage](#usage)
    - [Maven dependency](#maven-dependency)
    - [Framework servlets](#framework-servlets)
      - [JsonServlet](#jsonservlet)
      - [SpringJsonServlet](#springjsonservlet)
    - [Web module configuration](#web-module-configuration)
    - [Service implementation](#service-implementation)
    - [Service registration](#service-registration)
    - [Running](#running)
  - [Action life-cycle](#action-life-cycle)
  - [Implementation details](#implementation-details)
    - [Serialization](#serialization)
    - [Threading issues](#threading-issues)
    - [Schema customization](#schema-customization)
    - [Response object and error handling](#response-object-and-error-handling)
    - [HTTP response](#http-response)
      - [Status codes](#status-codes)
      - [Content-Type header](#content-type-header)
      - [Caching](#caching)
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
The following concepts are used throughout the rest of the documentation, and must be defined:
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
Just extend from [JsonAction](src/main/java/org/brutusin/jsonsrv/JsonAction.java), and ensure to define your input/output parameters as POJOs.

Examples:
```java
public class HelloWorldAction extends JsonAction<String, String> {
    @Override
    public String execute(String input) throws Exception {
        return "Hello " + input + "!";
    }
   
    @Override
    public boolean isCacheable() {
        return true;
    }
}
```

```java
public class GetDateAction extends JsonAction<Void, String> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat();

    @Override
    public String execute(Void input) throws Exception {
        return dateFormat.format(new Date());
    }

    public void setDatePattern(String pattern) {
        dateFormat = new SimpleDateFormat(pattern);
    }
}
```

###Framework servlets
Two alternative framework servlets are available, covering two different configuration scenarios: 
* [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java): Base servlet that loads service definitions from `jsonsrv.json` (explained later). No dependency injection supported.
* [SpringJsonServlet](src/main/java/org/brutusin/jsonsrv/SpringJsonServlet.java): Extending the previous servlet, this servlet loads the service definitions from [Spring](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/xsd-config.html) configuration XML files (by default `jsonsrv.xml`). Spring dependencies have a `<scope>provided</scope>` in this module, so in order to use this servlet, [org.springframework:spring-context](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.springframework%22%20a%3A%22spring-context%22) artifacts must be provided by the client module at runtime. 

####JsonServlet
#####Web module configuration
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

#####Service registration
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
This way, all requests under the `/srv` path will be processed by it.

####SpringJsonServlet
#####Web module configuration
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

#####Service registration
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

## Implementation details
###Serialization
Use POJOs as the service aguments/return types, and avoid cyclic references. See [jackson-databind documentation](https://github.com/FasterXML/jackson-databind) for more details.
###Threading issues
The framework creates a single action instance per service (action mapping in `jsonsrv.json`) to serve all requests, that is, actions will run on a multithreaded environment, so be aware that they must handle concurrent requests and be careful to synchronize access to shared resources.
###Schema customization
See [jackson-module-jsonSchema documentation](https://github.com/FasterXML/jackson-module-jsonSchema) for supported schema generation features, like:
* Required properties
* Property name
* Property description 

###Response object and error handling
All HTTP requests processed by the framework (via [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java)), return a JSON payload meeting the following schema:
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
being the `value` property schema dependent on the action queried. 

The following error codes are defined, aligned with the [JSON_RPC 2.0 specification](http://www.jsonrpc.org/specification#error_object):

Code | Meaning  | Description
------| ------- | ---------
-32700|Parse error|Invalid JSON was received by the server. An error occurred on the server while parsing the JSON input
-32601|Service not found|The service does not exist / is not available
-32602|Invalid input|Invalid service input. Returned when received input does not meet schema restrictions and when action `execute(..)` method throws an `IllegalArgumentException` 
-32000|Application error|Error contemplated by the application logic. In case of a checked exception thrown by action `execute(..)`
-32603|Internal error|In an internal error occurs or action `execute(..)` method throws an unchecked (runtime) exception.

###HTTP response
Although HTTP responses can be decorated using [custom renderers](#custom-renderers), the default behaviour is as follows:
####Status codes
Depending on the returned JSON payload, the following status codes are returned:

[HTTP response status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)| Case
------| ------- |
200/304 | if `error` is `null` or `error.code` equals `-32000`  (see [Caching section](#caching) for more details)
400 | if `error.code` equals `-32700` or `-32602`
404 | if `error.code` equals `-32601`)
500 | any other error

####Content-Type header
`Content-Type:application/json`

####Caching
The framework automatically handles caching depending on this three factors: 
* Execution without error
* Implementation of the `public boolean isCacheable()` method of the action (by default returns ´false´) 
* Conditional request header `If-None-Match` present

The algorithm is as follows:

* If an error occurred, no caching issues
* Else if action is cacheable
	* Compute response *etag* from as a hash of the JSON payload to be returned
	* If a `If-None-Match` header is present in the request and its value is equals to computed *etag* (meaning that client cache is fresh), set response status code to `304 (NOT MODIFIED)` and return no payload
	* Else, add response headers `Cache-Control:private` and `ETag` with the computed value and return the JSON payload in the HTTP response body. Additionally if the method is *POST* ([rfc7231](http://www.rfc-editor.org/rfc/rfc7231.txt) 4.3.3) add a `Content-Location` header to the *GET* url
* Else, return the following response headers:
```
Expires:Thu, 01 Jan 1970 00:00:00 GMT
Cache-Control:max-age=0, no-cache, no-store
Pragma:no-cache
```

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

Additionaly, only for [SpringJsonServlet](src/main/java/org/brutusin/jsonsrv/SpringJsonServlet.java):
* `spring-cfg`: Path to an additional (all `jsonsrv.xml` in classpath are always used) spring configuration file to use,

### Servlets overridable methods
The following  [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java) methods can be overriden:
* `protected ClassLoader getClassLoader()`: Lets specify a different *ClassLoader* for loading the pluggable resources (configuration file, action classes and render class). If not overridden, `JsonServlet.class.getClassLoader()` is returned.
* `protected ObjectMapper getObjectMapper()`: To use a custom [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/ObjectMapper.html) in the JSON to java binding.
* `protected SchemaFactoryWrapper getSchemaFactory()`: To use a custom [SchemaFactoryWrapper](https://github.com/FasterXML/jackson-module-jsonSchema/blob/master/src/main/java/com/fasterxml/jackson/module/jsonSchema/factories/SchemaFactoryWrapper.java) in the java-class to schema binding.
* `protected Map<String, JsonAction> loadActions()`: To change the source actions are loaded from.
* `protected List<String> getSupportedInitParams()`: If subclasses add new `init-param` parameters they have to be declared to be used. 

##Example:
A complete example project is available at [jsonsrv-example](https://github.com/brutusin/jsonsrv-example).  

##Main stack
This module could not be possible without:
* [FasterXML/jackson stack](https://github.com/FasterXML/jackson) The underlying JSON stack.
* [com.fasterxml.jackson.module:jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema) for java class to JSON schema mapping 
* [com.github.fge:json-schema-validator](https://github.com/fge/json-schema-validator) for validation against a JSON schema
* [Spring IoC](http://projects.spring.io/spring-framework/) Used optionally to load service mappings from Spring

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
