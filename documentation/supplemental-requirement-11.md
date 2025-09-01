# Supplemental Requirement 11: POST /api/v1/tradeOrders/bulk

The process of posting trade orders to the trade service is currently too slow.  The purpose of this enhancement is to allow trade orders to be posted in bulk.  This enhancement will create a new API endpoint, POST `/api/v1/tradeOrders/bulk`. It will work like POST `/api/v1/tradeOrders`, except that instead of taking a single `TradeOrderPostDTO`, it will take an array of them; and instead of returning a single `TradeOrderResponseDTO`, the response will include an array of response DTOs (plus an other messages).  This new API will insert the received trade orders into the trade_order table.  Use the existing POST `/api/v1/tradeOrders` as a guide.

Since performance is the primary goal of this change, the implementation of this enhancement should insert the tradeOrders into the database in a single insert statement.  The bulk submission should either succeed or fail as a unit.  We may change this in the future, but this will be the behavior for now.  It is expected that most inserts will succeed. 

Streamline the processing with this API as much as possible for optimal performance.  The trade service should be able to handle a large number of trade orders being posted at once.  This enhancement should not be a bottleneck.

The usual http status codes should be used.  Please return a detailed error message in the response body if the request fails.  Please also log the error with sufficient details that the cause can be understood.

Don't spend too much time on integration testing.  I will integrate test in Kubernetes.  It is quick and easy for me to test.