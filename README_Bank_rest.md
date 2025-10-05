# Система управления банковскими картами

# Bank REST API

Приложение REST API для управления пользователями, картами и транзакциями банка.

## Стек технологий

- Java 17+ / 22
- Spring Boot 3.2.x
- Spring Data JPA
- PostgreSQL
- Liquibase
- Docker / Docker Compose

---

## Запуск проекта

## Клонирование проекта
Клонируем репозиторий и переходим в папку проекта:

```bash
git clone <URL_REPOSITORY>
cd bank_rest
````
## Создание файла с секретным ключом для JWT
Для работы JWT нужен секретный ключ, который используется для подписи токенов.  
Создадим файл `jwt.secret` и положим в него.

```bash
openssl rand -base64 32 > jwt.secret
````
Эта команда создаст случайную 32-байтовую строку и сохранит её в jwt.secret

## Редактирование файла .env-example
Отредактируйте файл .env-example, который хранит настройки окружения.

## Запуск Docker контейнеров
Поднимаем Docker контейнеры с приложением и PostgreSQL:

```bash
docker compose up --build
```
Проверяем, что контейнер базы запущен:

```bash
docker ps
```

Должны быть контейнеры: `bank_postgres` и `bank_app`.

## Подключение к базе данных

```bash
docker exec -it bank_postgres psql -U postgres -d bank_db
```

Проверяем таблицы:

```sql
\dt
```

Должны быть таблицы: `users`, `cards`, `card_transactions`, `refresh_token`.

## Сброс базы данных (опционально)
Если нужно очистить базу и начать с нуля:

```bash
docker compose down -v
docker compose up --build
```

## Проверка работы Liquibase
После запуска новые таблицы должны создаться автоматически. Проверяем командой в psql:

```sql
\dt
```
Если таблицы есть — миграции отработали корректно.

