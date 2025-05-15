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
| version         | Integer               | Version for optimistic locking              |
| blotter         | BlotterResponseDTO    | Nested DTO for blotter                      |

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
