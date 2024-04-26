# FileCache

A simple file-cache as described in this [blog post](https://udby.com/archives/474).

It has the following responsibilities:

* Maintain cache of files in the temporary file system while not using too much space
* Ensure often-accessed content is cached
* Ensure an item is only produced once in a high-traffic scenario where the same resource is requested concurrently

## Requirements

* JDK21+ (any flavor)
* Maven 3.9+

## Implementation

The [file-cache implementation](file-cache-impl) contains only the core parts of the file-cache, along with tests.

## Test Setup

In the [test-setup](test-setup) folder there is a minimal spring-boot application exposing a single endpoint
serving PNG images containing the number given in the url.

Showcase how to configure the file-cache implementation in a spring-boot setup with configurable properties.

Eg [http://localhost:8080/api/images/numbers/1234567890](http://localhost:8080/api/images/numbers/1234567890) returns:

![1234567890.png](1234567890.png)

### Run it from command line:

```
mvn -pl test-setup -am spring-boot:run
```

It also has 2 static HTML pages, one covering the [first numbers 0..99](http://localhost:8080) and the other
sports [random numbers](http://localhost:8080/random.html).

### k6

In the [k6 folder](k6) there is a simple [Grafana K6](https://grafana.com/docs/k6/latest/) setup simulating up to 140 virtual users to stress the implementation a bit.
It requests random images where some will be cached and some will be generated to stress all parts of the implementation.

```
cd k6
k6 run api-test.js
```

_Have fun :-)_

(c) 2024 Jesper Udby
