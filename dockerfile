# Use an official Maven image to build the project
FROM maven:3.8.6-openjdk-8 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml file and install dependencies
COPY pom.xml /app/

COPY /lib/ /app/lib
# Download all dependencies into the Maven cache
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY /src/ /app/src


# Build the application
RUN mvn clean package -DskipTests

# Use a new minimal JDK image to run the application
FROM openjdk:8-jre

# Set the working directory in the container
WORKDIR /app

# Copy the built jar from the Maven image
COPY --from=build /app/target/support-application-jar-with-dependencies.jar /app/support-application-jar-with-dependencies.jar
COPY --from=build /app/src/main/resources/UBA-Logo.svg /app/UBA-Logo.svg
COPY --from=build /app/src/main/resources/Blank_Letter.jrxml /app/Blank_Letter.jrxml

# Expose the port the application will run on
EXPOSE 9897

# Set the entry point to run the jar file
ENTRYPOINT ["java", "-jar", "support-application-jar-with-dependencies.jar"]
