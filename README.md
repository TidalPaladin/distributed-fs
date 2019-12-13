# Distributed Filesystem
Files for a containerized distributed filesystem implemented
in Java. Created for CS6378 - Advanced Operating Systems.

## Prerequisites
* `docker`
* `docker-compose`
* (Optional) `maven` to build from source

## Usage

### Building from Source
To build a `.jar` file and Docker image, run 

```
mvn package
```

from the project root directory. Note that if `docker` is not
installed or unavailable the build process may produce errors,
but the JAR file should still be produced successfully. 
The built JAR file will be in the `target` directory.

### Running with Docker
A `docker-compose.yml` file is provided to automate the
setup and execution of multiple clients / servers. Simply run 

```
docker-compose up --scale client=2 --scale server=5
```

This will start the specified number of clients and servers. To 
attach to a client container and enter filesystem commands, run 

```
docker attach distributed-fs_client_1
```

Where the appended `_1` indicates the target container index.

To simulate a server failure, run 

```
docker pause distributed-fs_server_1
```

To restore the failed server, run

```
docker unpause distributed-fs_server_1
```

Again, the final number indicates the container index and can be
changed to simulate failure of specific servers.


### Running without Docker

To run without Docker, first export the following environment variables.

```
PORT=32000            # Port to use
META=metaserver.com   # Hostname of metaserver
```

Then run the JAR file with 

```
java -jar target/hw3-1.0-SNAPSHOT.jar <meta,server,client>
```

where the final argument specifies the mode of operation.
