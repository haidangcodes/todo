# Todo App

Full-stack todo application with AI voice-to-task and email reminders.

## Features

- **Authentication** - Email/password + Google OAuth2 login
- **Task Management** - CRUD with categories, priorities, deadlines, subtasks
- **Voice AI** - Create tasks by voice using MiniMax M2.7
- **Email Reminders** - Automatic Gmail reminders for due tasks
- **Confetti Animation** - Celebration when completing tasks

## Tech Stack

**Frontend:** Vanilla HTML/CSS/JS, Google Fonts (Cormorant Garamond, Inter)

**Backend:** Spring Boot 3.2, Spring Data JPA, Spring Security OAuth2, Spring Mail

**Database:** MySQL 8+

## Prerequisites

- Java 17+
- MySQL 8+ running on port 3306
- Google OAuth2 credentials
- MiniMax API key
- Gmail App Password (for email reminders)

## Setup

### 1. Create MySQL Database

```sql
CREATE DATABASE todo_db;
```

### 2. Configure Environment

Create `src/main/resources/application-oauth2.properties`:
```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google
```

Create `src/main/resources/application-mail.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

In `src/main/resources/application.properties`, set your MiniMax API key:
```properties
minimax.api.key=YOUR_MINIMAX_API_KEY
```

### 3. Get API Keys

**Google OAuth2:** https://console.cloud.google.com → APIs & Services → Credentials → OAuth 2.0 Client IDs

**MiniMax AI:** https://platform.minimax.io → API Keys

**Gmail App Password:** https://myaccount.google.com/security → App Passwords (requires 2FA enabled)

### 4. Run

```bash
mvn spring-boot:run
```

Open: http://localhost:8080/

## Project Structure

```
src/main/
├── java/com/todo/
│   ├── config/
│   │   ├── SecurityConfig.java    # OAuth2 + session auth
│   │   └── CorsConfig.java       # CORS configuration
│   ├── controller/
│   │   ├── TaskController.java   # /api/tasks CRUD
│   │   ├── AuthController.java   # /api/auth/*
│   │   └── AIController.java     # /api/ai/parse-task
│   ├── entity/
│   │   ├── Task.java            # Task entity
│   │   ├── User.java           # User entity
│   │   └── Subtask.java       # Embeddable subtask
│   ├── repository/
│   │   ├── TaskRepository.java
│   │   └── UserRepository.java
│   ├── scheduler/
│   │   └── ReminderScheduler.java  # Email reminders
│   └── service/
│       ├── TaskService.java
│       └── EmailService.java
└── resources/
    └── static/
        ├── index.html          # Login page
        ├── auth/index.html    # Login alias
        └── app/index.html     # Todo app

```

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Create account |
| POST | `/api/auth/login` | No | Login |
| GET | `/api/auth/me` | Yes | Current user |
| POST | `/logout` | Yes | Logout |
| GET | `/oauth2/authorization/google` | No | Google login |
| GET | `/api/tasks` | Yes | List tasks |
| POST | `/api/tasks` | Yes | Create task |
| PUT | `/api/tasks/{id}` | Yes | Update task |
| DELETE | `/api/tasks/{id}` | Yes | Delete task |
| PATCH | `/api/tasks/{id}/toggle` | Yes | Toggle complete |
| GET | `/api/tasks/filter/{filter}` | Yes | Filter: all/today/upcoming/overdue |
| POST | `/api/ai/parse-task` | Yes | Voice parse |
