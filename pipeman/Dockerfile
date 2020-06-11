################################################################################
###  Pipeline Manager docker image
#
# Coordinates the ingest, decoding and publishing of the image sequeces to be 
# procesed.
#
# This docker image is based on OpenJDK.
#
# When run, the container runs Java application contained in the 
# pipeman-1.0-SNAPSHOT.jar.
#
# Mount points:
#  - /var/run/docker.sock:/var/run/docker.sock so it can access the HOST's
#       docker enviromnent and access its sibling containers. 
#  - /jobs_in: contains the sequences of images ingested and to be encoded
#       into an MPEG-DASH stream
#  - /job_out: directory where the encoded stream will be output
#  - /encoders: directory where the encoder containers registers itself
#
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################

# This is a multistaged image. First use a maven image to build the java
# solution.
FROM maven:3.6.3-jdk-14 AS MAVEN_BUILD

COPY ./pom.xml ./pom.xml

RUN mvn dependency:go-offline -B

COPY ./src ./src
RUN mvn clean package

RUN mvn dependency:copy-dependencies -DoutputDirectory=/target \
    -Dhttps.protocols=TLSv1.2

# Then, create the actual target image based on OpenJDK and copy the application
# jar file and its dependencies.
FROM openjdk:15-jdk-alpine3.11
RUN apk upgrade -U \
    && apk add --no-cache ca-certificates docker

COPY --from=MAVEN_BUILD /target/pipeman-1.0-SNAPSHOT.jar \
    /pipeman-1.0-SNAPSHOT.jar
COPY --from=MAVEN_BUILD /target/javax.json-api-1.1.4.jar \
    /javax.json-api-1.1.4.jar
COPY --from=MAVEN_BUILD /target/javax.json-1.1.4.jar \
    /javax.json-1.1.4.jar

# Copy a shell script to reset all the contents of the managed volumes.
COPY ./reset_all.sh /reset_all.sh
RUN chmod a+x /reset_all.sh

# set the startup command to execute the jar
CMD ["java", "-jar", "/pipeman-1.0-SNAPSHOT.jar"]