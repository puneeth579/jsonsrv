#org.brutusin:jsonsrv [![Build Status](https://api.travis-ci.org/brutusin/jsonsrv.svg?branch=master)](https://travis-ci.org/brutusin/jsonsrv)
A lightweight, jar-packaged, self-describing, JSON RPC service framework for JEE (servlet-based) for easily exposing business methods through a JSON over HTTP API. 

Aimed at creating AJAX/JSON web interfaces.

**Table of Contents**

- [org.brutusin:jsonsrv](#orgbrutusinjsonsrv)
  - [Features](#features)
  - [Usage](#usage)
    - [Maven dependency](#maven-dependency)
    - [Web module configuration](#web-module-configuration)
    - [Service implementation](#service-implementation)
    - [Service registration](#service-registration)
    - [Running](#running)
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
    - [JsonServlet init params](#jsonservlet-init-params)
    - [JsonServlet overridable methods](#jsonservlet-overridable-methods)
  - [Example](#example)
  - [Main stack](#main-stack)
  - [Brutusin dependent modules](#brutusin-dependent-modules)
  - [Support, bugs and requests](#support-bugs-and-requests)
  - [Authors](#authors)
  - [License](#license)

##Features
* **Self-describing**. Input/output schemas of the service can be obtained using the `schema` url-parameter. Automatic form generation and validation is straightforward using components like https://github.com/exavolt/onde
* **Automatic conditional caching**. Just tell what is cacheable.
* **Easy implementation**. Just code your business.
* **Plugable rendering**. [Custom renderers](#custom-renderers) can be developed in order to provide more advanced visualizations. 

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

###Web module configuration
In the `web.xml` configure the following mapping for the framework servlet, [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java):
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

###Service implementation
Just extend from [JsonAction](src/main/java/org/brutusin/jsonsrv/JsonAction.java), and ensure to define your input/output parameters as POJOs.

Example:
```java
package org.brutusin.jsonsrv.example;

import org.brutusin.jsonsrv.JsonAction;

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

###Service registration
Register the actions in order to the framework can find them, by creating a `jsonsrv.json` file in the root namespace (so it can be loaded by `getClassLoader().getResources("jsonsrv.json")`).

Example:
```json
[
  {"id": "hello",    "className": "org.brutusin.jsonsrv.example.complex.HelloWorldAction"},
  {"id": "date",     "className": "org.brutusin.jsonsrv.example.GetDateAction"},
  {"id": "exception","className": "org.brutusin.jsonsrv.example.ExceptionAction"}
]
```

###Running

Run the web application and test it form the web browser. Both POST and GET methods are supported.

**Supported URL parameters**

URL parameter  | Description
------------- | -------------
**`id`** | Id of the service to execute, as registered in the `jsonsrv.json` file
**`input`** | json representation of the input
**`schema`** | Set it to `i` or `o` to return the schema of the input or output of the service respectively

**Use cases**

Case | URL  | Return
------| ------- | ---------
Service listing | `srv` | `{"value":["date","exception","hello","version"]}`
Service execution | `srv?id=example&input=%22world%22` | `{"value":"Hello world!"}`
Service input schema | `srv?id=example&schema=i` | `{"type":"string"}`
Service output schema | `srv?id=example&schema=o` | `{"type":"object","properties":{"error":{"type":"object","properties":{"code":{"type":"integer","required":true},"data":{"type":"any"},"meaning":{"type":"string","required":true},"message":{"type":"string","required":true}}},"value":{"type":"string"}}}`

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

Code | Meaning  | Data
------| ------- | ---------
-32700|Parse error|Invalid JSON was received by the server. An error occurred on the server while parsing the JSON input
-32601|Service not found|The service does not exist / is not available
-32602|Invalid input|Invalid service input. Received input does not meet schema restrictions
-32603|Internal error|Internal service error

###HTTP response
Although HTTP responses can be decorated using [custom renderers](#custom-renderers), the default behaviour is as follows:
####Status codes
Depending on the returned JSON payload, the following status codes are returned:

[HTTP response status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)| Case
------| ------- |
200/304 | if `error` is `null` (see [Caching section](#caching) for more details)
400 | if `error.code` equals `-32700` or `-32602`
404 | if `error.code` equals `-32601`)
500 | any other error

####Content-Type header
`Content-Type:application/json`

####Caching
The framework automatically handles caching issues depending on this three factors: 
* Execution without error
* Implementation of the `public boolean isCacheable()` method of the action (by default returns ´false´) 
* Conditional request header `If-None-Match` present

The algorithm is as follows:

* If an error occurred, no caching issues
* Else if action is cacheable
	* Compute response *etag* from as a hash of the JSON payload to be returned
	* If a `If-None-Match` header is present in the request and its value is equals to computed *etag* (meaning that client cache is valid), set response status code to `304 (NOT MODIFIED)` and return no payload
	* Else, add response header `ETag` with the computed value and return the JSON payload in the HTTP response body
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
More advanced functionality can be plugged using custom renderers; for example, [jsonsrv-human-renderer](https://github.com/brutusin/jsonsrv-human-renderer) module that adds and extra "human" mode that eases service testing and enhances readability, by the automatic creation of HTML forms from the input schema of the service.

In the next sections, it is explained how to configure a custom render. 

### JsonServlet init-params
The following optional init-params are supported by [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java):
* `schema-parameter-disabled`: Accepts a boolean value for disabling schema queries. Default value is `false` (enabled)
* `renderer`: Class name to the custom render to use. If not specified, the default renderer is used 
* `render-param`: Additional parameter to be passed to the custom renderer, accessible via its `getInitParam()` method

### JsonServlet overridable methods
The following  [JsonServlet](src/main/java/org/brutusin/jsonsrv/JsonServlet.java) methods can be overriden:
* `protected ClassLoader getClassLoader()`: Lets specify a different *ClassLoader* for loading the renderer class.
* `protected ObjectMapper getObjectMapper()`: To use a custom [ObjectMapper](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/com/fasterxml/jackson/databind/ObjectMapper.html) in the JSON to Java binding.

##Example:
A complete example project is available at [jsonsrv-example](https://github.com/brutusin/jsonsrv-example).  

##Main stack
This module could not be possible without:
* [FasterXML/jackson stack](https://github.com/FasterXML/jackson) The underlying JSON stack.
* [com.fasterxml.jackson.module:jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema) for java class to JSON schema mapping 
* [com.github.fge:json-schema-validator](https://github.com/fge/json-schema-validator) for validation against a JSON schema

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
