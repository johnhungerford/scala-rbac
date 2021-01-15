# Scala RBAC Examples

Example REST applications using `scala-rbac` for authorization. There are currently
examples for the *Play* framework and *Scalatra*.

## Building and running

The applications included here are configured to run from either a fat jar or a docker image.

### Fat jar

To build and run a fat jar for each application:

```bash
sbt assembly # will build jars for all projects
# or
sbt "project rbac[Name]" assembly

java -jar ./scala-rbac-examples/[name]-example/target/scala-2.12/scala-rbac-[name]-example_2.12-[version].jar [port]
```
where [name] is the simple name of the application (play, or scalatra), [version] is
the project version (see `build.sbt`), and [port] is the port you want to run the application
on (defaults to 9000).

### Docker image

To build and run a docker image for each application:

```bash
sbt docker:publishLocal # will build docker images for all projects
# or
sbt "project rbac[Name]" docker:publishLocal

docker run -p [port]:9000 johnhungerford/rbac-[name]-example:latest
```
where [name] is the simple name of the application (play, or scalatra), and [port] is
the port you want to run the application on.

You can also pull and run the image from docker hub without building using the following:

```bash
docker pull johnhungerford/rbac-[name]-example

docker run -p [port]:9000 johnhungerford/rbac-[name]-example:latest
```







