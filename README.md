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
