# TaMa – Task Management Application

TaMa is a personal task management web application built around boards, lists, and task cards. The project is inspired by Trello’s task organization approach and focuses on providing essential, intuitive, and user-friendly task management features.

This project was developed as part of the **Java Application Programming Practice** course at Ho Chi Minh City University of Technology – HUTECH.

> This project is intended for educational, demonstration, and academic project defense purposes. It has not been designed for production deployment.

---

## Main Features

### Authentication and Authorization

- User registration.
- Login and logout.
- Password encryption using BCrypt.
- Session-based authentication.
- Role-based authorization:
  - `USER`: access to personal task management features.
  - `ADMIN`: access to user account management features.

### Board Management

- Create new boards.
- View the current user's boards.
- Update board information.
- Delete boards.
- Set board background colors or images.

### List Management

- Create lists within a board.
- Rename lists.
- Delete lists.
- Reorder lists by position.

### Card Management

- Create cards within a list.
- Update card titles and descriptions.
- Delete cards.
- Move cards between lists within the same board.
- Reorder cards using drag-and-drop.
- Store card order using the `position` attribute.

### Card Details

- Task description.
- Due date.
- Completion status.
- Completion timestamp.
- Priority levels:
  - `LOW`
  - `MEDIUM`
  - `HIGH`
  - `URGENT`
- Task checklists.
- Comments.
- Comment editing and deletion.
- Labels assigned to cards.

### Label Management

- Create personal labels.
- Update labels.
- Delete labels.
- Reuse labels across multiple cards.
- Manage label colors.

### User Administration

- View the user account list.
- View user information.
- Manage account status or permissions within the system's supported scope.
- Restrict administrative features from regular users.

---

## Technologies Used

### Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Jakarta Validation
- Maven
- Lombok

### Frontend

- HTML
- CSS
- JavaScript
- Fetch API
- Thymeleaf
- Thymeleaf Layout Dialect
- Bootstrap

### Database

- MySQL

### Development Tools

- IntelliJ IDEA
- HeidiSQL
- Laragon
- Postman
- Git
- GitHub

---

## System Architecture

The project follows a layered architecture:

```text
Client
   │
   ▼
Controller / REST Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
MySQL Database
```

Responsibilities of each layer:

- **Controller:** receives requests and returns rendered pages or JSON responses.
- **Service:** handles business logic.
- **Repository:** accesses data using Spring Data JPA.
- **Entity:** maps Java objects to database tables.
- **DTO:** transfers data between the client and server through APIs.
- **Thymeleaf:** manages shared layouts and page structures.
- **JavaScript Fetch API:** calls REST APIs and updates the interface without reloading the entire page.

The project still follows the MVC model. Thymeleaf provides the initial pages and shared layouts, while JavaScript communicates with REST APIs to handle most business operations.

---

## Main Business Rules

- Each board belongs to only one user.
- Users can only manage boards that they own.
- Cards can only be moved between lists within the same board.
- Moving cards between different boards is not currently supported.
- List and card positions start from `0`.
- When positions change, the system normalizes the order of the affected elements.
- Labels belong to individual users and can be reused.
- Cards cannot currently be assigned to multiple members.
- Data is permanently deleted because soft deletion has not been implemented.
- The system does not currently store card change history.

---

## System Requirements

Before running the project, install the following software:

- JDK 17
- MySQL
- Git
- IntelliJ IDEA or another IDE with Maven support

Check the installed Java version using:

```bash
java -version
```

The result should indicate that Java 17 is installed.

---

## Project Installation

### 1. Clone the Repository

```bash
git clone https://github.com/longchau5823/tama_task_management.git
cd tama_task_management
```

Alternatively, download the source code as a ZIP file from GitHub and extract it.

---

### 2. Initialize the Database

Start MySQL using Laragon, HeidiSQL, or another database management tool.

Import the following file:

```text
database/tama_db.sql
```

The script creates the following database:

```text
tama_db
```

To import the database using HeidiSQL:

1. Connect to the MySQL server.
2. Select **File → Load SQL file**.
3. Select `database/tama_db.sql`.
4. Execute the script.
5. Verify that the required tables have been created.

---

### 3. Configure the Database Connection

The project supports database configuration through environment variables.

The following variables can be used:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Example Spring Boot configuration:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/tama_db}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

For the default MySQL configuration provided by Laragon:

```text
Database: tama_db
Username: root
Password: leave empty
Port: 3306
```

Do not commit real database passwords to the repository.

---

### 4. Run the Application

#### Using IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **Open**.
3. Select the project directory.
4. Wait for Maven to download the required dependencies.
5. Verify that the Project SDK is configured to use JDK 17.
6. Run the main class containing:

```java
@SpringBootApplication
```

#### Using Maven Wrapper on Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Using Git Bash, Linux, or macOS

```bash
./mvnw spring-boot:run
```

After the application starts successfully, open:

```text
http://localhost:8080
```

---

## User Accounts

New users can register at:

```text
http://localhost:8080/register
```

After registration, accounts are assigned the regular user role by default.

The repository does not publish a fixed administrator account or password. To test administrative features, grant the `ADMIN` role to an appropriate account in the database.

Do not store personal accounts, real passwords, or real user data in the SQL script.

---

## Project Structure

```text
tama_task_management/
├── .mvn/
│   └── wrapper/
│
├── database/
│   └── tama_db.sql
│
├── src/
│   └── main/
│       ├── java/
│       │   └── ...
│       │       ├── config/
│       │       ├── controller/
│       │       ├── dto/
│       │       ├── entity/
│       │       ├── repository/
│       │       ├── security/
│       │       └── service/
│       │
│       └── resources/
│           ├── static/
│           │   ├── css/
│           │   ├── images/
│           │   └── js/
│           │
│           ├── templates/
│           └── application.properties
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

The actual directory structure may differ slightly depending on the current package organization of the project.

---

## Current Scope

The current version focuses on personal task management.

The following features are outside the current project scope:

- Boards with multiple members.
- Assigning cards to members.
- Board sharing.
- Moving cards between different boards.
- Real-time notifications.
- Due-date reminder emails.
- Activity history.
- Soft deletion and data recovery.
- Real-time data synchronization.
- Mobile applications.
- Production deployment.

---

## Future Development

Possible future improvements include:

- Supporting multiple members within a board.
- Adding owner, administrator, and member permissions.
- Assigning cards to users.
- Sending notifications when cards are close to their due dates.
- Adding activity logs.
- Searching and filtering cards.
- Supporting file attachments.
- Implementing soft deletion and data recovery.
- Adding complete unit and integration test coverage.
- Dockerizing the application and database.
- Deploying the application to a cloud environment.
- Building a separate frontend using React or Vue.

---

## Author

**Châu Thuyên Long**  
Team Leader and Sole Developer

Responsibilities:

- System analysis and technical planning
- Database design
- Backend development
- Frontend development
- REST API implementation
- Authentication and authorization
- Testing, integration, and project maintenance

Ho Chi Minh City University of Technology – HUTECH  
Information Technology – Software Engineering

---

## Notes

This project was developed for educational purposes and to practice the following technologies and concepts:

- Spring Boot
- Spring MVC
- Spring Security
- RESTful APIs
- Spring Data JPA
- MySQL
- Thymeleaf
- JavaScript Fetch API
- Layered application architecture

Some components may continue to be modified and improved in future versions.
