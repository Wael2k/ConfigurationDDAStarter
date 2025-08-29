# Use official OpenJDK 19 image as base
FROM openjdk:19-oracle

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Copy source code
COPY src src

# Make mvnw executable
RUN chmod +x mvnw

# Build the application and skip tests for faster build
RUN ./mvnw clean package -DskipTests

# The jar is already in target folder inside the image
# Set the entrypoint to run the jar
ENTRYPOINT ["java","-jar","/app/target/your-artifact-name.jar"]

# Expose port 8080
EXPOSE 8080
