# TITAN Commerce — Business Requirements Document

**Document ID:** TITAN-BRD-001  
**Version:** 1.0  
**Status:** Approved for initial development  
**Project:** Project TITAN  
**Business Model:** B2C E-Commerce

---

## 1. Business Overview

TITAN Commerce is a fictional Business-to-Consumer (B2C) e-commerce company that sells general consumer products through an online shopping platform.

The platform allows customers to create accounts, browse products, manage shopping carts, place orders and track their order status.

The engineering objective is to build a reliable, secure, maintainable and cost-conscious application platform that supports these business operations.

Project TITAN will simulate the engineering environment of a growing e-commerce company.

## 2. Business Model

**Business-to-Consumer (B2C)**

TITAN Commerce sells products directly to individual customers.

The company operates a centralized product catalogue and order-processing system.

### Product Categories

The initial catalogue will support:

- Electronics
- Clothing
- Accessories
- General consumer products

The product catalogue will be extensible to support additional categories.

## 3. Business Actors

| Actor | Responsibilities |
|---|---|
| Customer | Register, authenticate, browse products, manage cart and place orders |
| Administrator | Manage products and view customer orders |
| Order Processing Worker | Process newly created orders and update their status |
| Engineering Team | Develop, deploy, secure, monitor and maintain the platform |

## 4. Customer Business Workflow

The primary customer journey is:

1. Customer visits the TITAN Commerce platform.
2. Customer registers for an account or logs in.
3. Customer browses the product catalogue.
4. Customer views product details.
5. Customer adds products to the shopping cart.
6. Customer reviews the cart and quantities.
7. Customer proceeds to checkout.
8. Customer submits the order.
9. The system generates a unique order ID.
10. The order is stored and submitted for background processing.
11. The order-processing worker processes the order.
12. The order status is updated.
13. The customer views the latest order status.

## 5. Functional Requirements

### 5.1 Customer Authentication

- FR-01: Customers must be able to register accounts.
- FR-02: Registered customers must be able to log in and log out.
- FR-03: The platform must authenticate customer requests.
- FR-04: Passwords must never be stored in plaintext.
- FR-05: Customers must only access their own protected account and order information.

### 5.2 Product Catalogue

- FR-06: Customers must be able to browse available products.
- FR-07: Customers must be able to view individual product details.
- FR-08: Product information must include name, description, price, category and availability.
- FR-09: Administrators must be able to create, update and manage product information.

### 5.3 Shopping Cart

- FR-10: Authenticated customers must be able to add products to their cart.
- FR-11: Customers must be able to update product quantities.
- FR-12: Customers must be able to remove products from their cart.
- FR-13: The platform must calculate the cart total accurately.

### 5.4 Order Management

- FR-14: Customers must be able to submit orders.
- FR-15: The platform must generate a unique order identifier.
- FR-16: Orders must contain customer, product, quantity and pricing information.
- FR-17: Orders must be persisted before being processed asynchronously.
- FR-18: Customers must be able to retrieve their order history.
- FR-19: Customers must be able to view the status of their orders.
- FR-20: Administrators must be able to view and manage orders.

### 5.5 Background Processing

- FR-21: Newly submitted orders must be available to a background processing component.
- FR-22: The worker must process orders independently of customer-facing API requests.
- FR-23: Processing failures must be logged.
- FR-24: The system must support safe retries without accidentally processing the same order more than once.

## 6. Order Lifecycle

The initial order lifecycle will support:

| Status | Meaning |
|---|---|
| PENDING | Order created and awaiting processing |
| PROCESSING | Background worker is processing the order |
| COMPLETED | Order processing completed successfully |
| FAILED | Order processing failed |

The initial version will simulate order fulfilment. Real courier and payment integrations are outside the initial scope.

## 7. Non-Functional Requirements

### 7.1 Availability and Reliability

- NFR-01: Application components must expose appropriate health endpoints.
- NFR-02: Services must recover gracefully from failures.
- NFR-03: Order processing must avoid accidental duplicate processing.
- NFR-04: Database persistence must protect successfully submitted orders against application restarts.

### 7.2 Performance

- NFR-05: API endpoints must respond within reasonable time under expected project test workloads.
- NFR-06: Background processing must not unnecessarily block customer-facing requests.
- NFR-07: Application performance must be measurable through testing and monitoring.

### 7.3 Security

- NFR-08: Apply least-privilege access controls.
- NFR-09: Store passwords using an appropriate secure password-hashing algorithm.
- NFR-10: Protect application secrets and credentials.
- NFR-11: Validate and sanitize incoming application data.
- NFR-12: Enforce authorization for protected customer and administrator operations.
- NFR-13: Never commit credentials, access keys or sensitive configuration to Git.

### 7.4 Scalability

- NFR-14: Application components should be independently deployable.
- NFR-15: The architecture should support independent scaling of the API and background worker.
- NFR-16: Infrastructure should be designed to support future growth.

### 7.5 Observability

- NFR-17: Application components must generate structured operational logs.
- NFR-18: Expose appropriate application and infrastructure metrics.
- NFR-19: Provide visibility into application health, errors and request performance.
- NFR-20: Document troubleshooting and recovery procedures.

### 7.6 Maintainability

- NFR-21: Maintain a modular and understandable application structure.
- NFR-22: Automate repeatable build, test and deployment activities.
- NFR-23: Maintain version-controlled infrastructure and application configuration.
- NFR-24: Document architectural decisions and operational procedures.

### 7.7 Cost Management

- NFR-25: Prefer local development and testing where practical.
- NFR-26: Avoid unnecessary cloud resources.
- NFR-27: Evaluate infrastructure costs before AWS deployment.
- NFR-28: Maintain resource cleanup and teardown procedures.

## 8. Initial Project Scope

### Included

- Customer registration and authentication.
- Product catalogue.
- Shopping cart.
- Order placement.
- Order persistence.
- Background order processing.
- Order status tracking.
- Basic administrator functionality.
- Containerization and local development.
- Infrastructure automation.
- CI/CD and deployment automation.
- Security scanning.
- Monitoring and operational documentation.

### Excluded from Initial Development

- Real payment gateway integration.
- Real courier or shipping integration.
- Mobile applications.
- Multi-vendor marketplace functionality.
- Production handling of real customer personal information.
- Advanced recommendation engines.

These features may be reconsidered in future versions.

## 9. Engineering Constraints and Assumptions

- TITAN is a fictional company and will use synthetic test data.
- The initial platform will be developed locally.
- AWS integration will be introduced selectively after cost and credit eligibility checks.
- Payment processing will be simulated.
- Product inventory and fulfilment will initially be simplified.
- The architecture and technology stack may evolve through documented Architecture Decision Records.

## 10. Business Success Criteria

The initial platform will be considered functionally successful when:

1. A customer can register and authenticate.
2. A customer can browse the product catalogue.
3. A customer can manage a shopping cart.
4. A customer can submit an order.
5. The system generates and persists a unique order ID.
6. A background worker can process the order.
7. The order status can be retrieved by its authorized customer.
8. Administrators can manage products and inspect orders.
9. The application can be built and run through documented local setup instructions.

The wider engineering success criteria will include automated deployments, security controls, observability, infrastructure automation and documented recovery procedures.

---

**Document Owner:** Project TITAN Engineering  
**Document Status:** Approved for initial development  
**Version:** 1.0
