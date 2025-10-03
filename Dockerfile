# 1. Базовый образ с JDK 17
FROM eclipse-temurin:17-jdk

# 2. Рабочая директория внутри контейнера
WORKDIR /app

# 3. Копируем jar-файл приложения
COPY target/bank-rest-0.0.1-SNAPSHOT.jar app.jar

# 4. Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
