FROM openjdk:17-slim

WORKDIR /app

RUN groupadd --system appuser && useradd --system --gid appuser --home-dir /app appuser

COPY backend/target/*.jar /app/app.jar
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
