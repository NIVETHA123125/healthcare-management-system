# CareGrid - Healthcare Management System

CareGrid is a modern, responsive, and flat-designed Healthcare Management System built using Spring Boot, Thymeleaf, Spring Data JPA, MySQL, and Lombok.

## Tech Stack
* **Backend:** Java 17+, Spring Boot 3.3.0, Spring MVC, Spring Data JPA
* **Frontend:** Thymeleaf templates, Vanilla CSS (Custom Bento layout & color scheme), Vanilla Javascript
* **Database:** MySQL
* **Utilities:** Lombok

## System Features
1. **Secure Session Authentication:** Local custom interceptor enforcing login session boundaries (excl. Spring Security).
2. **Dashboard Overview:** Displays clinic metrics (total patients, active doctors, scheduled/completed appointments) and today's schedule.
3. **Patient Registry (CRUD):** Add, view, edit, search, and delete patient records.
4. **Doctor Directory (CRUD):** Add, view, edit, search, and delete medical practitioners.
5. **Appointment Engine:** Interactive scheduling form with patient/doctor selection, status tracking (Scheduled -> Completed / Cancelled), and history view.

## Run Instructions

### Prerequisites
1. **Java 17 or higher** installed.
2. **Maven 3.8+** installed.
3. **MySQL Server** running.

### Database Setup
1. Create a MySQL schema named `healthcare_db`:
   ```sql
   CREATE DATABASE healthcare_db;
   ```
2. You can optionally seed or initialize tables using the provided [database.sql](file:///c:/Users/Nivetha/OneDrive/Desktop/health%20care%20management%20system/database.sql) file. However, Hibernate's `ddl-auto=update` is configured, meaning the tables will be created automatically upon application startup.
3. A default admin user is seeded automatically by the application on startup:
   * **Username:** `admin`
   * **Password:** `admin123`

### Build and Launch
1. Navigate to the project root directory.
2. Compile and package the application:
   ```bash
   mvn clean package
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
4. Access the web portal in your browser:
   * **URL:** [http://localhost:8080](http://localhost:8080)
   * **Login credentials:** Use `admin` and `admin123` to log in.
