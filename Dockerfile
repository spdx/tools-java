# syntax=docker/dockerfile:1.4

# Set Java versions
ARG JAVA_VERSION=17

# Use Maven eclipse Temurin based
FROM maven:3.8-eclipse-temurin-$JAVA_VERSION as build

ARG TOOLS_JAVA_VERSION=1.1.5-SNAPSHOT

WORKDIR /build

# BUILD
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=bind,source=$PWD,target=/build,rw \
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install \
    && mkdir -p /usr/lib/java/spdx \
    && cp target/tools-java-$TOOLS_JAVA_VERSION-jar-with-dependencies.jar /usr/lib/java/spdx/


# Configure the wrapper script
COPY scripts/tools-java-wrapper.sh /usr/bin/tools-java

# Make the bwrapper match tools version
RUN sed -i "s/@@VERSION@@/$TOOLS_JAVA_VERSION/g" /usr/bin/tools-java \
    && chmod +x /usr/bin/tools-java


# Deploy image
FROM eclipse-temurin:$JAVA_VERSION as run

COPY --from=build /usr/lib/java/spdx /usr/lib/java/spdx
COPY --from=build /usr/bin/tools-java /usr/bin/tools-java

ENTRYPOINT [ "/usr/bin/tools-java" ]