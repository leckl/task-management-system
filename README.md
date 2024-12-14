# Система Управления Задачами (Task Management System)

**Task Management System (TMS)** — это REST API для управления задачами с ролями пользователей и администраторов. Система позволяет создавать, редактировать, удалять и просматривать задачи, а также управлять их статусами, приоритетностью и комментариями. Включает возможности аутентификации и авторизации с использованием JWT.

---

## Функциональные возможности

- **Роли и права:**
    - **Администратор (ADMIN)**:
        - Управление всеми задачами (создание, редактирование, удаление).
        - Изменение статусов и приоритетов задач.
        - Назначение исполнителей.
        - Просмотр задач по авторам или исполнителям.
        - Добавление, удаление и изменение своих комментариев.
    - **Пользователь (USER)**:
        - Управление назначенными задачами (изменение статуса, добавление, удаление и изменение своих комментариев).
        - Просмотр задач, в которых он указан как исполнитель.

- **Управление задачами:**
    - Каждая задача включает:
        - Заголовок.
        - Описание.
        - Статус: "в ожидании", "в процессе", "завершено".
        - Приоритет: "высокий", "средний", "низкий".
        - Автор и исполнители.
        - Список комментариев.
    - Возможность фильтрации и пагинации задач определённого автора или исполнителя по приоритету и статусу.

- **Аутентификация и авторизация:**
    - Spring Security с поддержкой JWT.
    - Валидация данных и обработка ошибок с понятными сообщениями.

- **Документация API:**
    - Open API и Swagger UI.

---

## Технологический стек

- **Язык**: Java 17+
- **Фреймворки**: Spring Boot, Spring Security, Spring Web, Spring Data JPA
- **База данных**: PostgreSQL
- **Документация**: OpenAPI (Swagger)
- **Инструменты**:
    - Docker Compose для локального развертывания
    - JUnit и Mockito для тестирования
    - Hibernate для запросов к БД

---

## Установка и запуск

### 1. Клонирование репозитория

Склонируйте проект на локальную машину:

```bash
git clone https://github.com/leckl/task-management-system.git
cd task-management-system
```

### 2. Настройка окружения

Настройте параметры в файле application.properties, который находится в директории src/main/resources.

Пример конфигурации для базы данных:

```env
spring.datasource.url=jdbc:postgresql://db:5432/tasks_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
```

### 3. Запуск с помощью Docker Compose

Запустите проект в режиме разработки:

```bash
docker-compose up --build
```

Docker Compose автоматически поднимет:
- **PostgreSQL** (контейнер `db`).
- **Spring Boot API** (контейнер `app`).

После успешного запуска, API будет доступен по адресу:  
[http://localhost:8080](http://localhost:8080)

---

## Использование

### Эндпоинты

Примеры основных запросов:

1. **Регистрация пользователя**:
   ```http
   POST /auth/register
   Content-Type: application/json

   {
       "email": "user@example.com",
       "password": "password123"
   }
   ```

2. **Авторизация пользователя**:
   ```http
   POST /auth/login
   Content-Type: application/json

   {
       "email": "user@example.com",
       "password": "password123"
   }
   ```

3. **Получение роли администратора (для авторизированных пользователей)**:

   **ТОЛЬКО В ЦЕЛЯХ ТЕСТИРОВАНИЯ**
   ```http
   PATCH /auth/upgrade-to-admin
   ```
   
4. **Создание задачи (администратор)**:
   ```http
   POST /tasks
   Authorization: Bearer <JWT_TOKEN>
   Content-Type: application/json

   {
       "title": "Новая задача",
       "description": "Описание задачи",
       "priority": "высокий",
       "status": "в ожидании",
       "executorId": 2
   }
   ```
5. **Изменение статуса задачи (исполнитель или администратора)**:
   ```http
   PATCH /tasks/{id}/status
   Authorization: Bearer <JWT_TOKEN>
   Content-Type: application/json

   {
       "status": "в процессе"
   }
   ```

Полная документация API доступна по адресу:  
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Тестирование

Для запуска тестов выполните:

1. В контейнере Docker:
   ```bash
   docker-compose exec app mvn test
   ```
2. Локально:
   ```bash
   mvn test
   ```

---

## Структура проекта

```
task-management-system/
├── src/
│   ├── main/
│   │   ├── java/org/taskmanagementsystem/    # Основной код приложения
│   │   └── resources/                        # Конфигурация (application.properties)
│   └── test/                                 # Тесты
├── docker-compose.yml                        # Конфигурация Docker Compose
├── Dockerfile                                # Dockerfile для сборки приложения
├── pom.xml                                   # Конфигурация Maven
└── README.md                                 # Документация
```

---

## Возможные проблемы

1. **Проблема с портами**:  
   Если порт `8080` или `5432` уже используется, измените их в `docker-compose.yml` и `application.properties`.

2. **Ошибка при запуске**:  
   Убедитесь, что Docker и Docker Compose установлены и работают корректно.

3. **Проблемы с подключением к базе данных**:  
   Проверьте параметры базы данных в файле `application.properties` и настройте их корректно.

---

Этот `README.md` включает все необходимые инструкции для локального запуска и использования системы управления задачами.