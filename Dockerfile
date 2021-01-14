FROM openjdk:8

LABEL maintainer="John Hungerford"

ARG EXAMPLE_NAME

ENV APP_DIR /opt/app

RUN mkdir /opt/app

WORKDIR $APP_DIR

COPY "./scala-rbac-examples/$EXAMPLE_NAME-example/target/scala-*/*assembly*.jar" $APP_DIR/app.jar

RUN chmod -R 755 /opt/app

ENTRYPOINT ["java", "-jar", "app.jar"]
