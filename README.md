# Cinema Ticket Booking System

A Kotlin desktop application for cinema ticket booking, built with Jetpack Compose for Desktop and SQLite.

This project was developed as an individual assessment for the Object Oriented Design and Development module. It extends a group project by introducing a proper layered architecture, persistent storage, administrator functionality, and a modern graphical interface while preserving the original domain logic and discount rules.

## Features

- Film search by title or genre
- Screening selection with hall, date and time
- Interactive seat map (A1–A10) with real-time availability
- Ticket purchase with automatic discount calculation
- Administrator dashboard (login protected)
  - View all films and screenings
  - Add new films with screenings
  - Dynamic price management using a multiplication factor
  - Enable/disable special offers (persistent)
- 25% Morning Discount (before noon on weekdays)
- 30% Group Booking Discount (applied from the 3rd seat)
- Morning discount is always applied first when both offers are eligible

## Technology & Architecture

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose for Desktop + Material 3
- **Database**: SQLite
- **Architecture**: Clean layered architecture (Model / Data / Service / UI)
- **Design Patterns**: Repository, Service Layer, Template Method, Sealed Result Type
- **Testing**: JUnit 5 with 47 tests using in-memory databases

## Getting Started

### Run the Application
1. Open the project in IntelliJ IDEA
2. Let Gradle sync
3. Run `MainKt`

**Administrator credentials** (seeded):

## Screenshots

| Welcome Screen | Film Search & Seat Selection |
|----------------|------------------------------|
| [Welcome] | [Film Search] |

| Discount Application | Admin Dashboard |
|----------------------|-----------------|
| [Discounts] | [Admin] |
- Username: `admin`
- Password: `Admin123`

### Run Tests
Right-click `src/test/CinemaSystemTest.kt` → Run

All 47 tests should pass.

## Project Structure
