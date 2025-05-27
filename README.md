# globeco-trade-service
Trade service for the GlobeCo suite for benchmarking Kubernetes autoscaling.

## Blotter Data Model

The **blotter** table represents a logical grouping of trades. Each blotter has an abbreviation, a name, and a version for optimistic locking.

### Entity Fields
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the blotter     |
| name          | String  | Name of the blotter              |
| version       | Integer | Version for optimistic locking   |

### DTOs

#### BlotterResponseDTO (Response)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the blotter     |
| name          | String  | Name of the blotter              |
| version       | Integer | Version for optimistic locking   |

#### BlotterPutDTO (PUT Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the blotter     |
| name          | String  | Name of the blotter              |
| version       | Integer | Version for optimistic locking   |

#### BlotterPostDTO (POST Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| abbreviation  | String  | Abbreviation for the blotter     |
| name          | String  | Name of the blotter              |

## Blotter API

All endpoints are prefixed with `/api/v1`.

| Verb   | URI                        | Request DTO         | Response DTO           | Description                                 |
|--------|----------------------------|---------------------|------------------------|---------------------------------------------|
| GET    | /api/v1/blotters           |                     | [BlotterResponseDTO]   | Get all blotters                            |
| GET    | /api/v1/blotters/{id}      |                     | BlotterResponseDTO     | Get a single blotter by ID                  |
| POST   | /api/v1/blotters           | BlotterPostDTO      | BlotterResponseDTO     | Create a new blotter                        |
| PUT    | /api/v1/blotters/{id}      | BlotterPutDTO       | BlotterResponseDTO     | Update an existing blotter by ID            |
| DELETE | /api/v1/blotters/{id}?version={version} |         |                        | Delete a blotter by ID and version          |

### Example Request/Response

#### Create Blotter (POST)
```
POST /api/v1/blotters
Content-Type: application/json
{
  "abbreviation": "EQ",
  "name": "Equity"
}
```

#### Response
```
HTTP/1.1 201 Created
{
  "id": 1,
  "abbreviation": "EQ",
  "name": "Equity",
  "version": 1
}
```

#### Get All Blotters (GET)
```
GET /api/v1/blotters
```

#### Response
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "abbreviation": "EQ",
    "name": "Equity",
    "version": 1
  }
]
```

## TradeOrder Data Model

The **trade_order** table represents an order to trade a security. Each trade order has an orderId, portfolioId, orderType, securityId, quantity, limitPrice, tradeTimestamp, version for optimistic locking, and a reference to a blotter.

### Entity Fields
| Field           | Type           | Description                                 |
|-----------------|----------------|---------------------------------------------|
| id              | Integer        | Unique identifier                           |
| orderId         | Integer        | Order identifier                            |
| portfolioId     | String         | Portfolio identifier                        |
| orderType       | String         | Order type                                  |
| securityId      | String         | Security identifier                         |
| quantity        | BigDecimal     | Quantity ordered                            |
| limitPrice      | BigDecimal     | Limit price                                 |
| tradeTimestamp  | OffsetDateTime | Timestamp of the trade                      |
| submitted       | Boolean        | Whether the order has been submitted (default: false, nullable) |
| version         | Integer        | Version for optimistic locking              |
| blotterId       | Integer        | Foreign key to blotter                      |

### DTOs

#### TradeOrderResponseDTO (Response)
| Field           | Type                  | Description                                 |
|-----------------|----------------------|---------------------------------------------|
| id              | Integer               | Unique identifier                           |
| orderId         | Integer               | Order identifier                            |
| portfolioId     | String                | Portfolio identifier                        |
| orderType       | String                | Order type                                  |
| securityId      | String                | Security identifier                         |
| quantity        | BigDecimal            | Quantity ordered                            |
| limitPrice      | BigDecimal            | Limit price                                 |
| tradeTimestamp  | OffsetDateTime        | Timestamp of the trade                      |
| blotter         | BlotterResponseDTO    | Nested DTO for blotter                      |
| submitted       | Boolean               | Whether the order has been submitted (default: false, nullable) |
| version         | Integer               | Version for optimistic locking              |

#### TradeOrderPutDTO (PUT Request)
| Field           | Type           | Description                                 |
|-----------------|----------------|---------------------------------------------|
| id              | Integer        | Unique identifier                           |
| orderId         | Integer        | Order identifier                            |
| portfolioId     | String         | Portfolio identifier                        |
| orderType       | String         | Order type                                  |
| securityId      | String         | Security identifier                         |
| quantity        | BigDecimal     | Quantity ordered                            |
| limitPrice      | BigDecimal     | Limit price                                 |
| tradeTimestamp  | OffsetDateTime | Timestamp of the trade                      |
| version         | Integer        | Version for optimistic locking              |
| blotterId       | Integer        | Foreign key to blotter                      |

#### TradeOrderPostDTO (POST Request)
| Field           | Type           | Description                                 |
|-----------------|----------------|---------------------------------------------|
| orderId         | Integer        | Order identifier                            |
| portfolioId     | String         | Portfolio identifier                        |
| orderType       | String         | Order type                                  |
| securityId      | String         | Security identifier                         |
| quantity        | BigDecimal     | Quantity ordered                            |
| limitPrice      | BigDecimal     | Limit price                                 |
| tradeTimestamp  | OffsetDateTime | Timestamp of the trade                      |
| blotterId       | Integer        | Foreign key to blotter                      |

## TradeOrder API

All endpoints are prefixed with `/api/v1`.

| Verb   | URI                              | Request DTO            | Response DTO                | Description                                 |
|--------|-----------------------------------|------------------------|-----------------------------|---------------------------------------------|
| GET    | /api/v1/tradeOrders              |                        | [TradeOrderResponseDTO]     | Get all trade orders                        |
| GET    | /api/v1/tradeOrders/{id}         |                        | TradeOrderResponseDTO       | Get a single trade order by ID              |
| POST   | /api/v1/tradeOrders              | TradeOrderPostDTO      | TradeOrderResponseDTO       | Create a new trade order                    |
| PUT    | /api/v1/tradeOrders/{id}         | TradeOrderPutDTO       | TradeOrderResponseDTO       | Update an existing trade order by ID        |
| DELETE | /api/v1/tradeOrders/{id}?version={version} |                |                             | Delete a trade order by ID and version       |

### Example Request/Response

#### Create TradeOrder (POST)
```
POST /api/v1/tradeOrders
Content-Type: application/json
{
  "orderId": 12345,
  "portfolioId": "PORTFOLIO1",
  "orderType": "BUY",
  "securityId": "SEC123",
  "quantity": 100.00,
  "limitPrice": 10.50,
  "tradeTimestamp": "2024-06-01T12:00:00Z",
  "blotterId": 1
}
```

#### Response
```
HTTP/1.1 201 Created
{
  "id": 1,
  "orderId": 12345,
  "portfolioId": "PORTFOLIO1",
  "orderType": "BUY",
  "securityId": "SEC123",
  "quantity": 100.00,
  "limitPrice": 10.50,
  "tradeTimestamp": "2024-06-01T12:00:00Z",
  "submitted": false,
  "version": 1,
  "blotter": {
    "id": 1,
    "abbreviation": "EQ",
    "name": "Equity",
    "version": 1
  }
}
```

#### Get All TradeOrders (GET)
```
GET /api/v1/tradeOrders
```

#### Response
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "orderId": 12345,
    "portfolioId": "PORTFOLIO1",
    "orderType": "BUY",
    "securityId": "SEC123",
    "quantity": 100.00,
    "limitPrice": 10.50,
    "tradeTimestamp": "2024-06-01T12:00:00Z",
    "submitted": false,
    "version": 1,
    "blotter": {
      "id": 1,
      "abbreviation": "EQ",
      "name": "Equity",
      "version": 1
    }
  }
]
```

## Submit Trade Order API

### Endpoint

`POST /api/v1/tradeOrders/{id}/submit`

Submits a trade order for execution. The request body must include:
- `quantity`: The amount to submit (must not exceed available quantity)
- `destinationId`: The destination for the execution

### Business Rules
- The value of `quantity` in the payload may not exceed `trade_order.quantity - trade_order.quantity_sent`. If it does, the API returns 400 Bad Request with the message: "Requested quantity exceeds available quantity".
- On success, `trade_order.quantity_sent` is increased by the submitted `quantity`.
- `trade_order.submitted` is set to true only if `trade_order.quantity == trade_order.quantity_sent`.

### Example Response

```
{
  "id": 1,
  "orderId": 123456,
  "portfolioId": "PORT1",
  "orderType": "BUY",
  "securityId": "SEC1",
  "quantity": 100.00,
  "quantitySent": 10.00,
  "limitPrice": 10.00,
  "tradeTimestamp": "2024-06-10T12:00:00Z",
  "blotter": { "id": 1, "abbreviation": "EQ", "name": "Equity", "version": 1 },
  "submitted": false,
  "version": 1
}
```

### Fields
| Field         | Type      | Description                                      |
|--------------|-----------|--------------------------------------------------|
| quantity     | decimal   | Total order quantity                             |
| quantitySent | decimal   | Cumulative quantity submitted for execution      |
| submitted    | boolean   | True if all quantity has been submitted          |

### Error Response
If the requested quantity exceeds available quantity:
```
HTTP/1.1 400 Bad Request
{
  "error": "Requested quantity exceeds available quantity"
}
```

## Destination Data Model

The **destination** table represents a trading destination (e.g., broker or exchange). Each destination has an abbreviation, a description, and a version for optimistic locking.

### Entity Fields
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the destination |
| description   | String  | Description of the destination   |
| version       | Integer | Version for optimistic locking   |

### DTOs

#### DestinationResponseDTO (Response)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the destination |
| description   | String  | Description of the destination   |
| version       | Integer | Version for optimistic locking   |

#### DestinationPutDTO (PUT Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the destination |
| description   | String  | Description of the destination   |
| version       | Integer | Version for optimistic locking   |

#### DestinationPostDTO (POST Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| abbreviation  | String  | Abbreviation for the destination |
| description   | String  | Description of the destination   |

## Destination API

All endpoints are prefixed with `/api/v1`.

| Verb   | URI                              | Request DTO            | Response DTO                | Description                                 |
|--------|-----------------------------------|------------------------|-----------------------------|---------------------------------------------|
| GET    | /api/v1/destinations             |                        | [DestinationResponseDTO]    | Get all destinations                        |
| GET    | /api/v1/destinations/{id}        |                        | DestinationResponseDTO      | Get a single destination by ID              |
| POST   | /api/v1/destinations             | DestinationPostDTO     | DestinationResponseDTO      | Create a new destination                    |
| PUT    | /api/v1/destinations/{id}        | DestinationPutDTO      | DestinationResponseDTO      | Update an existing destination by ID        |
| DELETE | /api/v1/destinations/{id}?version={version} |                |                             | Delete a destination by ID and version       |

### Example Request/Response

#### Create Destination (POST)
```
POST /api/v1/destinations
Content-Type: application/json
{
  "abbreviation": "ML",
  "description": "Merrill Lynch"
}
```

#### Response
```
HTTP/1.1 201 Created
{
  "id": 1,
  "abbreviation": "ML",
  "description": "Merrill Lynch",
  "version": 1
}
```

#### Get All Destinations (GET)
```
GET /api/v1/destinations
```

#### Response
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "abbreviation": "ML",
    "description": "Merrill Lynch",
    "version": 1
  }
]
```

## TradeType Data Model

The **trade_type** table represents a type of trade (e.g., Buy, Sell, Short, Cover, Exercise). Each trade type has an abbreviation, a description, and a version for optimistic locking.

### Entity Fields
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the trade type  |
| description   | String  | Description of the trade type    |
| version       | Integer | Version for optimistic locking   |

### DTOs

#### TradeTypeResponseDTO (Response)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the trade type  |
| description   | String  | Description of the trade type    |
| version       | Integer | Version for optimistic locking   |

#### TradeTypePutDTO (PUT Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the trade type  |
| description   | String  | Description of the trade type    |
| version       | Integer | Version for optimistic locking   |

#### TradeTypePostDTO (POST Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| abbreviation  | String  | Abbreviation for the trade type  |
| description   | String  | Description of the trade type    |

## TradeType API

All endpoints are prefixed with `/api/v1`.

| Verb   | URI                          | Request DTO         | Response DTO             | Description                                 |
|--------|------------------------------|---------------------|--------------------------|---------------------------------------------|
| GET    | /api/v1/tradeTypes           |                     | [TradeTypeResponseDTO]   | Get all trade types                         |
| GET    | /api/v1/tradeType/{id}       |                     | TradeTypeResponseDTO     | Get a single trade type by ID               |
| POST   | /api/v1/tradeTypes           | TradeTypePostDTO    | TradeTypeResponseDTO     | Create a new trade type                     |
| PUT    | /api/v1/tradeType/{id}       | TradeTypePutDTO     | TradeTypeResponseDTO     | Update an existing trade type by ID         |
| DELETE | /api/v1/tradeType/{id}?version={version} |         |                          | Delete a trade type by ID and version        |

### Example Request/Response

#### Create TradeType (POST)
```
POST /api/v1/tradeTypes
Content-Type: application/json
{
  "abbreviation": "BUY",
  "description": "Buy"
}
```

#### Response
```
HTTP/1.1 201 Created
{
  "id": 1,
  "abbreviation": "BUY",
  "description": "Buy",
  "version": 1
}
```

#### Get All TradeTypes (GET)
```
GET /api/v1/tradeTypes
```

#### Response
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "abbreviation": "BUY",
    "description": "Buy",
    "version": 1
  }
]
```

## ExecutionStatus Data Model

The **execution_status** table represents the status of an execution (e.g., New, Sent, Filled, Cancelled). Each execution status has an abbreviation, a description, and a version for optimistic locking.

### Entity Fields
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the status      |
| description   | String  | Description of the status        |
| version       | Integer | Version for optimistic locking   |

### DTOs

#### ExecutionStatusResponseDTO (Response)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the status      |
| description   | String  | Description of the status        |
| version       | Integer | Version for optimistic locking   |

#### ExecutionStatusPutDTO (PUT Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| id            | Integer | Unique identifier                |
| abbreviation  | String  | Abbreviation for the status      |
| description   | String  | Description of the status        |
| version       | Integer | Version for optimistic locking   |

#### ExecutionStatusPostDTO (POST Request)
| Field         | Type    | Description                      |
|-------------- |---------|----------------------------------|
| abbreviation  | String  | Abbreviation for the status      |
| description   | String  | Description of the status        |

## ExecutionStatus API

All endpoints are prefixed with `/api/v1`.

| Verb   | URI                                      | Request DTO                | Response DTO                   | Description                                 |
|--------|-------------------------------------------|----------------------------|--------------------------------|---------------------------------------------|
| GET    | /api/v1/executionStatuses                |                            | [ExecutionStatusResponseDTO]   | Get all execution statuses                  |
| GET    | /api/v1/executionStatuses/{id}           |                            | ExecutionStatusResponseDTO     | Get a single execution status by ID         |
| POST   | /api/v1/executionStatuses                | ExecutionStatusPostDTO     | ExecutionStatusResponseDTO     | Create a new execution status               |
| PUT    | /api/v1/executionStatuses/{id}           | ExecutionStatusPutDTO      | ExecutionStatusResponseDTO     | Update an existing execution status by ID   |
| DELETE | /api/v1/executionStatuses/{id}?version={version} |                        |                                | Delete an execution status by ID and version |

### Example Request/Response

#### Create ExecutionStatus (POST)
```
POST /api/v1/executionStatuses
Content-Type: application/json
{
  "abbreviation": "NEW",
  "description": "New"
}
```

#### Response
```
HTTP/1.1 201 Created
{
  "id": 1,
  "abbreviation": "NEW",
  "description": "New",
  "version": 1
}
```

#### Get All ExecutionStatuses (GET)
```
GET /api/v1/executionStatuses
```

#### Response
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "abbreviation": "NEW",
    "description": "New",
    "version": 1
  }
]
```

## Execution Data Model

The **execution** table represents an execution of a trade order, including status, quantities, prices, and relationships to other entities.

### Entity Fields
| Field              | Type           | Description                                 |
|--------------------|----------------|---------------------------------------------|
| id                 | Integer        | Unique identifier                           |
| executionTimestamp | OffsetDateTime | Timestamp of execution                      |
| executionStatus    | ExecutionStatus| Status of the execution (FK)                |
| blotter            | Blotter        | Blotter (FK, nullable)                      |
| tradeType          | TradeType      | Trade type (FK, nullable)                   |
| tradeOrder         | TradeOrder     | Trade order (FK)                            |
| destination        | Destination    | Destination (FK)                            |
| quantityOrdered    | BigDecimal          | Quantity ordered                            |
| quantityPlaced     | BigDecimal     | Quantity placed                             |
| quantityFilled     | BigDecimal     | Quantity filled                             |
| limitPrice         | BigDecimal     | Limit price                                 |
| executionServiceId | Integer        | ID from the external execution service (nullable) |
| version            | Integer        | Version for optimistic locking              |

### DTOs

#### ExecutionResponseDTO (Response)
| Field              | Type                        | Description                                 |
|--------------------|----------------------------|---------------------------------------------|
| id                 | Integer                     | Unique identifier                           |
| executionTimestamp | OffsetDateTime              | Timestamp of execution                      |
| executionStatus    | ExecutionStatusResponseDTO  | Nested DTO for execution status             |
| blotter            | BlotterResponseDTO          | Nested DTO for blotter                      |
| tradeType          | TradeTypeResponseDTO        | Nested DTO for trade type                   |
| tradeOrder         | TradeOrderResponseDTO       | Nested DTO for trade order                  |
| destination        | DestinationResponseDTO      | Nested DTO for destination                  |
| quantityOrdered    | BigDecimal                       | Quantity ordered                            |
| quantityPlaced     | BigDecimal                  | Quantity placed                             |
| quantityFilled     | BigDecimal                  | Quantity filled                             |
| limitPrice         | BigDecimal                  | Limit price                                 |
| executionServiceId | Integer                     | ID from the external execution service (nullable) |
| version            | Integer                     | Version for optimistic locking              |

#### ExecutionPutDTO (PUT Request)
| Field              | Type           | Description                                 |
|--------------------|----------------|---------------------------------------------|
| id                 | Integer        | Unique identifier                           |
| executionTimestamp | OffsetDateTime | Timestamp of execution                      |
| executionStatusId  | Integer        | Foreign key to execution status             |
| blotterId          | Integer        | Foreign key to blotter                      |
| tradeTypeId        | Integer        | Foreign key to trade type                   |
| tradeOrderId       | Integer        | Foreign key to trade order                  |
| destinationId      | Integer        | Foreign key to destination                  |
| quantityOrdered    | BigDecimal          | Quantity ordered                            |
| quantityPlaced     | BigDecimal     | Quantity placed                             |
| quantityFilled     | BigDecimal     | Quantity filled                             |
| limitPrice         | BigDecimal     | Limit price                                 |
| executionServiceId | Integer        | ID from the external execution service (nullable) |
| version            | Integer        | Version for optimistic locking              |

#### ExecutionPostDTO (POST Request)
| Field              | Type           | Description                                 |
|--------------------|----------------|---------------------------------------------|
| executionTimestamp | OffsetDateTime | Timestamp of execution                      |
| executionStatusId  | Integer        | Foreign key to execution status             |
| blotterId          | Integer        | Foreign key to blotter                      |
| tradeTypeId        | Integer        | Foreign key to trade type                   |
| tradeOrderId       | Integer        | Foreign key to trade order                  |
| destinationId      | Integer        | Foreign key to destination                  |
| quantityOrdered    | BigDecimal          | Quantity ordered                            |
| quantityPlaced     | BigDecimal     | Quantity placed                             |
| quantityFilled     | BigDecimal     | Quantity filled                             |
| limitPrice         | BigDecimal     | Limit price                                 |
| executionServiceId | Integer        | ID from the external execution service (nullable) |

### Execution APIs

| Verb   | URI                              | Request DTO            | Response DTO                | Description                                 |
|--------|-----------------------------------|------------------------|-----------------------------|---------------------------------------------|
| GET    | /api/v1/executions               |                        | [ExecutionResponseDTO]      | Get all executions                          |
| GET    | /api/v1/executions/{id}          |                        | ExecutionResponseDTO        | Get a single execution by ID                |
| POST   | /api/v1/executions               | ExecutionPostDTO       | ExecutionResponseDTO        | Create a new execution                      |
| PUT    | /api/v1/executions/{id}          | ExecutionPutDTO        | ExecutionResponseDTO        | Update an existing execution by ID          |
| DELETE | /api/v1/executions/{id}?version={version} |                |                             | Delete an execution by ID and version        |

#### Example: Create Execution (POST)
```json
{
  "executionTimestamp": "2024-06-10T12:00:00Z",
  "executionStatusId": 1,
  "blotterId": 1,
  "tradeTypeId": 1,
  "tradeOrderId": 1,
  "destinationId": 1,
  "quantityOrdered": 10,
  "quantityPlaced": 100.00,
  "quantityFilled": 0.00,
  "limitPrice": 10.00,
  "executionServiceId": 55555
}
```

#### Example: Execution Response (GET)
```json
{
  "id": 1,
  "executionTimestamp": "2024-06-10T12:00:00Z",
  "executionStatus": { "id": 1, "abbreviation": "NEW", "description": "New", "version": 1 },
  "blotter": { "id": 1, "abbreviation": "EQ", "name": "Equity", "version": 1 },
  "tradeType": { "id": 1, "abbreviation": "BUY", "description": "Buy", "version": 1 },
  "tradeOrder": { "id": 1, "orderId": 123456, "portfolioId": "PORT1", "orderType": "BUY", "securityId": "SEC1", "quantity": 100.00, "limitPrice": 10.00, "tradeTimestamp": "2024-06-10T12:00:00Z", "version": 1, "blotter": { "id": 1, "abbreviation": "EQ", "name": "Equity", "version": 1 } },
  "destination": { "id": 1, "abbreviation": "ML", "description": "Merrill Lynch", "version": 1 },
  "quantityOrdered": 10,
  "quantityPlaced": 100.00,
  "quantityFilled": 0.00,
  "limitPrice": 10.00,
  "executionServiceId": 55555,
  "version": 1
}
```

## Health Check APIs

The service exposes standard health check endpoints for Kubernetes:

| Verb | URI                        | Description                | Example Response         |
|------|----------------------------|----------------------------|-------------------------|
| GET  | /api/v1/health/liveness    | Liveness probe             | { "status": "UP" }    |
| GET  | /api/v1/health/readiness   | Readiness probe            | { "status": "UP" }    |
| GET  | /api/v1/health/startup     | Startup probe              | { "status": "UP" }    |

All endpoints return HTTP 200 OK and a JSON body indicating the service is up.
