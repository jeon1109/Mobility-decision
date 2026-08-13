FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon \
    && find build/libs -type f -name '*.jar' ! -name '*-plain.jar' \
       -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system \
       --gid spring \
       --no-create-home \
       spring

COPY --from=builder \
     --chown=spring:spring \
     /workspace/app.jar \
     /app/app.jar

USER spring:spring

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

EXPOSE 9091

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]