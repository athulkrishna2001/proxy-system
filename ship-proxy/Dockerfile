# Use an official OpenJDK runtime as the base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the ShipProxy Java source file into the container
COPY ShipProxy.java .

# Compile the Java code
RUN javac ShipProxy.java

# Expose port 8080 for the ShipProxy (as specified in the code)
EXPOSE 8080

# Command to run the ShipProxy
CMD ["java", "com.example.proxy.ShipProxy"]