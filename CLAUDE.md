# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack todo app with Google OAuth2 + email/password authentication, AI voice-to-task, and email reminders:
- **Login page** (`/`) - Two-column layout: email form + Google sign-in
- **Todo App** (`/app`) - Full CRUD with categories, priorities, deadlines, subtasks, voice AI, email reminders
- **Backend** - Spring Boot 3.2 + Supabase PostgreSQL + Spring Security OAuth2 + Spring Mail

## Commands

### Development
```bash
# Load env vars then run (env vars must be exported, not in .env.local automatically)
set -a && source .env.local && set +a && mvn spring-boot:run
```

### Environment Variables (`.env.local`)
| Variable | Description |
|----------|-------------|
| `DB_HOST` | Supabase project host (xxx.supabase.co) |
| `DB_USER` | PostgreSQL user (postgres) |
| `DB_PASSWORD` | Supabase database password |
| `MINIMAX_API_KEY` | MiniMax AI API key |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `GMAIL_USERNAME` | Gmail address for SMTP |
| `GMAIL_APP_PASSWORD` | Gmail App Password |

### URLs
- Login: `http://localhost:8080/`
- Todo app: `http://localhost:8080/app`

## Architecture

### Backend (`src/main/java/com/todo/`)
```
config/
  SecurityConfig.java     # OAuth2 + session auth, route protection
  CorsConfig.java         # CORS for frontend
controller/
  TaskController.java     # REST API /api/tasks (user-scoped)
  AuthController.java     # /api/auth/*
  AIController.java       # /api/ai/parse-task (MiniMax voice→structured task)
  WelcomeController.java # Route redirects
scheduler/
  ReminderScheduler.java  # @Scheduled 1min — sends email reminders
service/
  TaskService.java        # Business logic
  EmailService.java       # Gmail SMTP sender
entity/
  Task.java              # @ManyToOne User, @ElementCollection subtasks
  User.java             # googleId, email, name, password, pictureUrl
  Subtask.java          # @Embeddable
repository/
  TaskRepository.java    # JPA + custom reminder query
  UserRepository.java    # findByEmail, findByGoogleId
```

### Frontend (`src/main/resources/static/`)
- `index.html` - Login page (coral gradient, two-column)
- `auth/index.html` - Alias of index.html
- `app/index.html` - Todo app (light theme, sidebar, FAB, voice mic)

### Task Data Model
Tasks are scoped to the logged-in user via `@ManyToOne User`. All `/api/tasks` queries filter by `session.userId`.

### AI Voice-to-Task
Browser uses Web Speech API (`vi-VN`) → transcript → `POST /api/ai/parse-task` → MiniMax M2.7 returns structured JSON (title, description, category, priority, deadline) → modal pre-filled.

### Email Reminders
`ReminderScheduler` runs every 1 minute. Finds tasks where `reminder <= now`, `reminderSent = false`, `completed = false` → sends Gmail → sets `reminderSent = true`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create email/password account |
| POST | `/api/auth/login` | Email/password login |
| GET | `/api/auth/me` | Current user info |
| POST | `/api/auth/logout` | End session |
| GET | `/oauth2/authorization/google` | Start Google OAuth login |
| GET | `/api/tasks` | List user's tasks |
| POST | `/api/tasks` | Create task |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |
| PATCH | `/api/tasks/{id}/toggle` | Toggle completion |
| GET | `/api/tasks/filter/{filter}` | Filter: all/today/upcoming/overdue |
| GET | `/api/tasks/category/{category}` | Filter by category |
| GET | `/api/tasks/priority/{priority}` | Filter by priority |
| POST | `/api/ai/parse-task` | Parse voice transcript to task JSON |

## Design

**Login Page**: Coral/peach gradient (#E07A5F, #F4A261), Cormorant Garamond serif + Inter, animated floating shapes, mouse glow, two-column layout

**Todo App**: Light (#fafafa), coral accent (#E07A5F), sidebar with filters (All/Today/Upcoming/Overdue/Categories/Priorities), voice mic button (bottom-right), confetti on completion, staggered animations

## Tech Stack
- Spring Boot 3.2, Spring Data JPA, Spring Security OAuth2 Client, Spring Mail
- **Database**: Supabase PostgreSQL (PostgreSQL driver, `org.hibernate.dialect.PostgreSQLDialect`)
- Vanilla HTML/CSS/JS frontend (no framework)
- Web Speech API for voice capture
- MiniMax M2.7 (Anthropic-compatible) for AI parsing
- Google Fonts (Cormorant Garamond, Inter)

## Getting Started

1. Create Supabase project at supabase.com
2. Configure `.env.local` with Supabase connection info and API keys
3. Run: `set -a && source .env.local && set +a && mvn spring-boot:run`
4. Open `http://localhost:8080/`
