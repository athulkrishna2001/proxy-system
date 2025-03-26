# Use an official OpenJDK runtime as the base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the OffshoreProxy Java source file into the container
COPY OffshoreProxy.java .

# Compile the Java code
RUN javac OffshoreProxy.java

# Expose port 9090 for the OffshoreProxy (as specified in the code)
EXPOSE 9090

# Command to run the OffshoreProxy
CMD ["java", "com.example.proxy.OffshoreProxy"]