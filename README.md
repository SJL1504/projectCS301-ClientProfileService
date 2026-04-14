## Introduction
This is Client Profile Service that I worked on as part of a group project for a banking CRM system hosted by UBS for a module

There are other services including:
- agent-admin service
- notifications service
- client-ai service
- authentication service
- client transactions
- logging service
- client acoount service

## credits
original repository is on privated repository
team members github users:
- Varidhi123456
- YumaTheThird
- rose-cider
- geraldloi
- TJWSMU

## Features

### Create Client Profile
- Generates a unique client ID and creates a new client profile.
- Validates agent ownership.
- Publishes creation events to an AWS SNS topic for auditing.

### Update Client Profile
- Updates client profile fields such as:
  - First name, last name, date of birth, gender
  - Email, phone number
  - Address, city, state, country, postal code
- Performs validation for unique email and phone number.
- Publishes granular update events for each modified field.

### Patch Client Status
- Update only the status of a client profile (e.g., PENDING, ACTIVE, DELETED).
- Publishes the status change asynchronously to the logging system.

### Delete Client Profile
- Soft deletes the client profile (status set to `DELETED`) instead of removing it from the database.
- Deletes all associated client accounts via the `ClientAccountClient`.
- Publishes delete events for audit purposes.

### Retrieve Client Profile(s)
- Fetch a single client profile by client ID.
- Fetch all client profiles associated with an agent.
- Each read operation is logged for audit purposes.

### Verification
- Sends verification emails to the client’s email address.
- Logs verification events asynchronously.

### Existence Check
- Checks whether a client exists and is assigned to a specific agent.
- Useful for validation before performing operations like creating accounts or updates.


## To Run App:
- type mvn spring-boot:run in same directory as pom.xml
- open localhost:8082 to access
- for local profile use mvn spring-boot:run -Dspring-boot.run.profiles=local
- for prod profile use mvn spring-boot:run -Dspring-boot.run.profiles=prod
