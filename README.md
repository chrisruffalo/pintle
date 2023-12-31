# Pintle

## What?
Pintle is a forwarding DNS server. In naval parlance a _pintle_ is the
connecting piece on the rudder that holds it into the gudgeon. Having
a rudder is a cornerstone of navigation and direction. A pintle is a
fundamental glue piece in providing the rudder and so, _pintle_ helps
you safely navigate the internet by providing correct direction to name
servers.

## Why?
Against my better judgement I have decided to follow up my smash-hit 
[gudgeon](https://github.com/chrisruffalo/gudgeon) with a Java version. 
Golang never felt like it had the tooling or the structures to support
what I wanted to do and the number of bugs continued to grow and grow.

## No, but really, _Why_?
The story of gudgeon (and, thus, pintle) starts back when my children were first
starting to use devices that were connected to the real internet. There was also
a need to translate certain domain names to specific IPs on a consistent basis
like sending all google search traffic from certain computers to the google safe-search
IP. I also wanted a way to combine different blocking behavior with different devices. 
I also wanted to be able to block different types of advertisements based on different
device characteristics. Then came the problem of IoT devices and separating that
traffic from the rest. I also wanted to be able to split my traffic across
different providers and use TLS/SSL to resolve queries all in the name of privacy.
Finally, I wanted to ensure that traffic to my ISP's servers went through
the ISP's DNS system. This allows automatic login in some cases.

Since I am not a BIND wizard this meant creating a DNS server that was built
for these purposes first and not created from different off-the-shelf products.
It needed to be designed as a blocking forwarder from the ground up. (Apparently
I think I am a Java wizard...)

## Architecture
Pintle is built on top of Quarkus and uses Quarkus and Smallrye features to
manage redundancy and resiliancy. It makes full use of many of the standards
provided by Quarkus including JAX-RS and JPA. It also uses a lot of Ver.x features
like the in-memory event bus and reactive-based sockets. Pintle also makes use
of other features like Smallrye fault-tolerance and configuration providers.

The basic operation of Pintle is that it recieves a DNS message packet on
one of the listeners (tcp or udp) and then it sends it as a message between
different event handlers. These handlers are each responsible for both handling
a certain stage (caching, blocking, resolution, etc) and coordinating with the
next stage in the DNS resolving pipeline. When a resolution completes the
results are logged and statistics are tabulated (if those features are enabled).

## Usage
TODO: get actual installation procedures, quarkus-app, uber-jar, native, deb, rpm, etc.

## Building

### Requirements
- JDK 21 (I use mandrel 23.1.r21)
- Maven 3.8.6 (required by Quarkus 3.4.2)

### Extensions
You will need to build and install the extensions in the ./extensions folder first. To
do this simply change directories into each and run `mvn clean install`.

### Running in Developer Mode
```shell
[]$ mvn clean quarkus:dev -Dquarkus.args='-c sample-config.yml'
```

### Building in Java Mode
```shell
[]$ mvn clean install
```

### Building a Native Executable
```shell
[]$ mvn clean install -Pnative
```
