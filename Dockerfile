FROM maven:3.8.3-openjdk-17

WORKDIR /app

COPY . .
RUN mvn clean install

EXPOSE 8081

CMD ["java", "-jar", "target/App.jar"]
