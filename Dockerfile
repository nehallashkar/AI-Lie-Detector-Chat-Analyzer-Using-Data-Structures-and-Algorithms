# Use lightweight Java 21 JDK Alpine image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy only the source files into the container
COPY src/ /app/src/

# Compile the Java source files
RUN mkdir out && javac -d out src/*.java

# Expose the default port (Render will override this dynamically via the $PORT env var)
EXPOSE 5000

# Run the Server class
CMD ["java", "-cp", "out", "src.Server"]
