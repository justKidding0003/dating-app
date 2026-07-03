# Java Standards

Java 21

Spring Boot 3

Use

- Constructor Injection

- Records for DTOs where appropriate

- Validation annotations

- Global Exception Handler

- Service Layer

- Repository Layer

Package Structure

feature

controller

service

repository

entity

dto

mapper

exception

Use

ResponseEntity

Never return entities directly.

Always return DTOs.

Write unit tests for business logic.