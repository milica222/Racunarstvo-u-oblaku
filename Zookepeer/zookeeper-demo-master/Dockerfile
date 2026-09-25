FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /build/target/bkatwal-zookeeper-demo-1.0-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8081 ZK_URL=zookeeper:2181 LEADER_ALGO=2
ENTRYPOINT ["sh", "-c", "exec java -Djava.awt.headless=true -Dserver.port=$SERVER_PORT -Dzk.url=$ZK_URL -Dleader.algo=$LEADER_ALGO -jar app.jar"]
