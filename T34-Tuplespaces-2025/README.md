# TupleSpaces

Distributed Systems Project 2025

**Group T34**

**Difficulty level: I am Death incarnate!**

### Team Members

| Number | Name              | User                             | Email                               |
|--------|-------------------|----------------------------------|-------------------------------------|
| 102598  | Alexandre Ramos | <https://github.com/alexandre-piedade-ramos>   | <mailto:alexandre.piedade.ramos@tecnico.ulisboa.pt>   |
| 107249  | David Rogério       | <https://github.com/DavidRogerio>     | <mailto:david.rogerio@tecnico.ulisboa.pt>     |
| 103193  | Rodrigo Cerejeira     | <https://github.com/RodrigoVelaCerejeira> | <mailto:rodrigo.vela.cerejeira@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules.
The definition of messages and services is in _Contract_.

See the [Project Statement](https://github.com/tecnico-distsys/Tuplespaces-2025) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

#### Server

**NOTE**: the default arguments provided to the program are defined in `pom.xml` as follows:

```s
<server.port> 3001
<debug>: -debug
```

Each server listens on a different port (3001, 3002, 3003) :

```
mvn -f SingleServer exec:java -Dexec.args="3001 [-debug]"
mvn -f SingleServer exec:java -Dexec.args="3002 [-debug]"
mvn -f SingleServer exec:java -Dexec.args="3003 [-debug]"
```


#### Front-End

**NOTE**: the default arguments provided to the program are defined in `pom.xml` as follows:

```s
<frontend.port>: 2001
<server.host>: localhost
<server.port>: 3001
<server.port>: 3002
<server.port>: 3003
<debug>: -debug
```

To run the front-end:

```s
mvn -f Front-End exec:java
```
or
```s
mvn -f Front-End exec:java -Dexec.args="2001 localhost:3001 localhost:3002 localhost:3003 [-debug]"
```

#### Client

**NOTE**: the default arguments provided to the program are defined in `pom.xml` as follows:

```s
<server.host>: localhost
<server.port>: 2001
<client.id>: 1
<debug>: -debug
```

To run the client:

```s
mvn -f Client-Java exec:java
```
or
```s
mvn -f Client-Java exec:java -Dexec.args="<server.host>:<server.port> <client.id> [-debug]"
```

#### Python Client

To setup the python virtual environment:

```s
python -m venv .venv
source .venv/bin/activate
python -m pip install grpcio
python -m pip install grpcio-tools
```

To create Python Client gRPC related classes:
```s
Contract/
mvn -f Contract install
mvn -f Contract exec:exec
```

To run the Python Client:
```s
python3 Client-Python/client_main.py <server.host>:<server.port> <client_id> [-debug]
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
