# Architecture Rules

## Service Ownership

Each business capability is a separate microservice.

Identity

User

Social Graph

Private List

Matching

Chat

Reveal

Fair Play

Notification

## Database

Database per Service.

No shared schema.

No direct database access across services.

## Communication

REST in Version 1.

Kafka in Version 2.

## API Gateway

Every request enters through Gateway.

Gateway performs

- JWT validation
- Routing
- Rate limiting

## Social Graph

All external integrations belong here.

Phone Contacts

LinkedIn

Instagram (subject to platform APIs)

QR

Invite Links

Business services never import contacts directly.

## Matching

Matching depends only on mutual Private List inclusion.

Matching never considers activity.

Matching never considers premium users.

## Private List

Exactly 25 users.

Maximum 3 replacements/day.

Unlimited reorder.