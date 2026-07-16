# syntax=docker/dockerfile:1.4

# SPDX-FileCopyrightText: Copyright (c) 2023 Source Auditor Inc.
# SPDX-FileType: SOURCE
# SPDX-License-Identifier: Apache-2.0

# Set Java versions
ARG JAVA_VERSION=21

# Use Maven eclipse Temurin based
FROM maven:3.9-eclipse-temurin-$JAVA_VERSION AS build

WORKDIR /build

# Copy the wrapper script, @@VERSION@@ to be replaced below
COPY scripts/tools-java-wrapper.sh /usr/bin/tools-java

# BUILD
# NOTE: TOOLS_JAVA_VERSION must be exported and used within single RUN.
# Env vars set with `export` do not persist across separate RUN layers.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=bind,target=/build,rw \
    # bind source defaults to the build context, expected to be the repo root
    export TOOLS_JAVA_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout) \
    && mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install \
    && mkdir -p /usr/lib/java/spdx \
    && cp target/tools-java-$TOOLS_JAVA_VERSION-jar-with-dependencies.jar /usr/lib/java/spdx/ \
    && sed -i "s/@@VERSION@@/$TOOLS_JAVA_VERSION/g" /usr/bin/tools-java \
    && chmod +x /usr/bin/tools-java

# Deploy image
FROM eclipse-temurin:$JAVA_VERSION AS run

COPY --from=build /usr/lib/java/spdx /usr/lib/java/spdx
COPY --from=build /usr/bin/tools-java /usr/bin/tools-java

ENTRYPOINT [ "/usr/bin/tools-java" ]
