FROM openjdk:8

LABEL maintainer="John Hungerford"

ARG MODULE_PATH

ENV APP_DIR /opt/app

RUN mkdir /opt/app

WORKDIR $APP_DIR

COPY ./$MODULE_PATH/target/scala-*/*assembly*.jar $APP_DIR/app.jar

RUN chmod -R 755 /opt/app

ENTRYPOINT ["java", "-jar", "app.jar"]
