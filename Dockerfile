## Etapa 1: Compilar o projeto (build Maven)
#FROM maven:3.9.6-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY pom.xml .
#COPY src ./src
#RUN mvn -q clean package -DskipTests
#
## Etapa 2: Imagem de runtime (JRE leve)
#FROM eclipse-temurin:17-jre-jammy
#ENV JAVA_OPTS=""
#WORKDIR /app
#
## Copia o jar gerado pelo Maven (assume único artefato em target/)
#COPY --from=build /app/target/*.jar /app/app.jar
#
## Porta do Spring Boot (definida em application.properties)
#EXPOSE 8045
#
## Inicia a aplicação (permite passar JAVA_OPTS em runtime)
#ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
#COPY --from=build /app/target/*.jar app.jar
#EXPOSE 8045
#ENTRYPOINT ["java", "-jar", "app.jar"]

# Etapa 1: Compilar o projeto (build Maven)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: Imagem de runtime (JRE leve)
FROM eclipse-temurin:17-jre-jammy
ENV JAVA_OPTS=""
WORKDIR /app

# Copia o jar gerado pelo Maven (assume único artefato em target/)
COPY --from=build /app/target/*.jar /app/app.jar

# Porta do Spring Boot
EXPOSE 8045

# Inicia a aplicação (permite passar JAVA_OPTS em runtime)
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]