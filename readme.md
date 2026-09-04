# Explore with me

## Студент Лютов Сергей Фёдорович, 69 кагорта


---
## Архитектура проекта

Проект состоит из 5 микросервисов и 3 инфраструктурных компонентов.

### Инфраструктура

| Компонент | Назначение | Порт |
|-----------|------------|------|
| Eureka Discovery Server | Регистрация и поиск сервисов | 8761 |
| Config Server | Централизованное управление конфигурациями | 0 (регистрируется в Eureka) |
| Gateway Server | Единая точка входа для всех запросов | 8080 |

### Микросервисы

| Сервис | Назначение | БД |
|--------|------------|-----|
| **user-service** | Управление пользователями | `ewm-users` (6543) |
| **rating-service** | Лайки/дизлайки и рейтинг событий | `ewm-ratings` (6544) |
| **request-service** | Управление заявками на участие | `ewm-requests` (6545) |
| **event-service** | Управление событиями, категориями, подборками | `ewm-events` (6546) |
| **stats-service** | Сбор и хранение статистики просмотров | `ewm-stats` (6542) |

---

## Взаимодействие между сервисами

Сервисы общаются через **Feign-клиенты** с использованием **Eureka Discovery**.

**event-service** вызывает:
- `user-service` — проверка существования пользователя
- `request-service` — получение количества подтверждённых заявок
- `stats-service` — получение количества просмотров

**rating-service** вызывает:
- `user-service` — проверка существования пользователя
- `event-service` — проверка события и обновление рейтинга

**request-service** вызывает:
- `user-service` — проверка существования пользователя
- `event-service` — проверка события

**Защита от сбоев:**  
Каждый Feign-клиент оснащён FallbackFactory и Resilience4j (Circuit Breaker + Retry).

---

##  Внутренний API

Внутренние эндпоинты недоступны через Gateway, используются только внутри кластера.

### user-service

| Эндпоинт | Метод | Назначение |
|----------|-------|------------|
| `/internal/users/by-ids` | POST | Получить пользователей по списку ID |

### event-service

| Эндпоинт | Метод | Назначение |
|----------|-------|------------|
| `/internal/events/by-ids` | POST | Получить события по списку ID |
| `/internal/events/{eventId}/rate` | POST | Обновить рейтинг события |

### request-service

| Эндпоинт | Метод | Назначение |
|----------|-------|------------|
| `/internal/requests/count/by-events` | POST | Получить количество заявок по списку событий |

### rating-service

| Эндпоинт | Метод | Назначение |
|----------|-------|------------|
| `/internal/ratings/by-events` | POST | Получить рейтинги по списку событий |

---

##  **Внешний API (спецификация OpenAPI):**

- [Основное API](ewm-main-service-spec.json)
- [Статистика API](ewm-stats-service-spec.json)

## Конфигурация

Все конфигурации вынесены в **Config Server** -
config-server/src/main/resources/config/

### Порядок запуска

1. Discovery Server (порт 8761)
2. Config Server
3. Gateway Server (порт 8080)
4. Stats Server
5. User Service
6. Rating Service
7. Request Service
8. Event Service
