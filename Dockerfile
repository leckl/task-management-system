# Шаг 1: Сборка приложения
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Копируем проект в контейнер
COPY . .

# Выполняем сборку с помощью Maven (можно адаптировать для Gradle)
RUN ./mvnw clean package -DskipTests

# Шаг 2: Создание минимального образа для выполнения
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Копируем только JAR-файл из предыдущего шага
COPY --from=build /app/target/*.jar app.jar

# Указываем порт, который будет слушать приложение
EXPOSE 8080

# Команда для запуска Spring Boot приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
