FROM eclipse-temurin:21-jdk
LABEL authors="denes"
WORKDIR /app
COPY build/libs/*.jar instantpayment.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","instantpayment.jar"]
