# Newsify-ktor

Newsify-ktor is the backend service for a modern news platform, built with Kotlin and the Ktor framework. It provides a robust API for user registration, authentication, content creation, and social interactions. The system is designed with a clear distinction between 'Readers' who consume content and 'Creators' who produce it.

## Project Overview

The application is built using a modern, layered architecture:
- **Controllers:** Handle incoming HTTP requests and responses.
- **Services:** Contain the core business logic.
- **Repositories:** Manage data persistence and interaction with the database.

This separation of concerns is managed by the **Koin** dependency injection framework, making the codebase modular, testable, and easy to maintain.

## Core Features

- **User Authentication:** Secure registration with email verification (via Stytch OTP), and JWT-based login with access and refresh tokens.
- **Role-Based Access Control (RBAC):** Distinct `READER` and `CREATOR` roles manage permissions for different API endpoints.
- **Content Management:** Creators can create, read, update, and delete their own news articles.
- **Social Following System:** Readers can follow their favorite creators to build a personalized content feed.
- **Push Notifications:** Integrated with Firebase Cloud Messaging (FCM) to notify followers when a creator publishes a new article.
- **Database Migrations:** Uses **Flyway** for safe, version-controlled database schema evolution.
- **API Documentation:** Automatically generated and interactive API documentation available via **Swagger UI** at the `/swaggerUI` endpoint.

## Technology Stack

- **Framework:** Ktor
- **Language:** Kotlin
- **Database:** PostgreSQL with Exposed Framework
- **Dependency Injection:** Koin
- **Authentication:** JWT
- **Testing:** JUnit 4, Testcontainers, MockK
- **External Services:**
    - Stytch (for OTP)
    - Firebase Cloud Messaging (for Push Notifications)

## API Endpoint Overview

The API provides a RESTful interface for interacting with the service. Here are some of the key endpoints:

- `POST /v1/authentication/register`: Register a new user.
- `POST /v1/authentication/login`: Log in to receive JWT tokens.
- `POST /v1/authentication/verify`: Verify a user's email with an OTP.
- `POST /v1/creator/create-news`: (Creator only) Create a new news article.
- `PATCH /v1/creator/updateNewsArticle/{creatorId}/{articleId}`: (Creator only) Partially update an article.
- `POST /v1/reader/follow/{creatorId}`: (Reader only) Follow a creator.
- `GET /v1/reader/articles-by-creators`: (Reader only) Get articles from a list of followed creators.

For a complete and interactive list of all endpoints, run the application and navigate to `/swaggerUI`.

## Setup & Configuration

1.  **Prerequisites:**
    - JDK 17 or higher.
    - Docker (for running the database and integration tests).

2.  **Configuration:**
    The application is configured via `src/main/resources/application.conf`. You will need to provide environment variables for sensitive data. Create a `.env` file in the project root with the following keys:

    ```dotenv
    # Database Configuration
    DB_ADDRESS="jdbc:postgresql://localhost:5432/newsify"
    DB_USER="your_db_user"
    DB_PASSWORD="your_db_password"

    # JWT Configuration
    JWT_SECRET="your-super-secret-jwt-key"
    JWT_ISSUER="your-issuer"
    JWT_AUDIENCE="your-audience"
    JWT_EXPIRATION_MINUTES="15"

    # Stytch Configuration (for OTP)
    STYTCH_PROJECT_ID="your-stytch-project-id"
    STYTCH_SECRET="your-stytch-secret"

    # Firebase Configuration (for Push Notifications)
    # Ensure the GOOGLE_APPLICATION_CREDENTIALS environment variable is set to the path of your Firebase service account JSON file.
    FIREBASE_PROJECT_ID="your-firebase-project-id"
    ```

## Building & Running

To build or run the project, use one of the following tasks:

| Task            | Description                                                          |
|-----------------|----------------------------------------------------------------------|
| `./gradlew test`| Run the tests                                                        |
| `./gradlew run` | Run the server using the configuration from your `.env` file.        |
| `./gradlew build`| Build everything                                                     |
| `./gradlew buildFatJar`| Build an executable JAR of the server with all dependencies included |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
