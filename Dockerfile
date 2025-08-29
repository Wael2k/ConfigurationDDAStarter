# =========================================================================
# Stage 1: Build Stage
# =========================================================================
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project definition file (pom.xml) first.
COPY pom.xml .

# Copy the Maven wrapper
COPY .mvn/ .mvn
COPY mvnw .
COPY mvnw.cmd .

# Copy the rest of the application's source code
COPY src ./src

# Run the Maven package command to build the JAR.
# -DskipTests skips running tests during the build, which is common for Docker builds.
RUN mvn package -DskipTests


# =========================================================================
# Stage 2: Final Image Stage
# =========================================================================
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app



# Expose the port the application runs on (default for Spring Boot is 8080)
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]

