#!/bin/bash

# Test script to verify the batch execution fix
echo "Testing batch execution submission fix..."

# Start the application in the background
echo "Starting application..."
./gradlew bootRun > app.log 2>&1 &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 30

# Check if application is running
if ! curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "Application failed to start. Check app.log for details."
    kill $APP_PID 2>/dev/null
    exit 1
fi

echo "Application started successfully!"

# Create some test data first (trade orders)
echo "Creating test trade orders..."

# Create trade order 1
curl -X POST http://localhost:8082/api/v1/tradeOrders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 12345,
    "portfolioId": "TEST_PORTFOLIO",
    "orderType": "BUY",
    "securityId": "TEST_SECURITY_1",
    "quantity": 100,
    "limitPrice": 50.00,
    "blotterId": 1
  }' > /dev/null

# Create trade order 2
curl -X POST http://localhost:8082/api/v1/tradeOrders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 12346,
    "portfolioId": "TEST_PORTFOLIO",
    "orderType": "BUY",
    "securityId": "TEST_SECURITY_2",
    "quantity": 200,
    "limitPrice": 75.00,
    "blotterId": 1
  }' > /dev/null

# Get the trade order IDs
echo "Getting trade order IDs..."
TRADE_ORDERS=$(curl -s http://localhost:8082/api/v1/tradeOrders | jq -r '.[] | select(.orderId == 12345 or .orderId == 12346) | .id')

if [ -z "$TRADE_ORDERS" ]; then
    echo "Failed to create test trade orders"
    kill $APP_PID 2>/dev/null
    exit 1
fi

# Convert to array
TRADE_ORDER_ARRAY=($TRADE_ORDERS)
TRADE_ORDER_1=${TRADE_ORDER_ARRAY[0]}
TRADE_ORDER_2=${TRADE_ORDER_ARRAY[1]}

echo "Created trade orders: $TRADE_ORDER_1, $TRADE_ORDER_2"

# Test batch submission
echo "Testing batch submission..."
BATCH_RESPONSE=$(curl -s -X POST http://localhost:8082/api/v1/tradeOrders/batch/submit \
  -H "Content-Type: application/json" \
  -d "{
    \"submissions\": [
      {
        \"tradeOrderId\": $TRADE_ORDER_1,
        \"quantity\": 50,
        \"destinationId\": 1
      },
      {
        \"tradeOrderId\": $TRADE_ORDER_2,
        \"quantity\": 100,
        \"destinationId\": 1
      }
    ]
  }")

echo "Batch response: $BATCH_RESPONSE"

# Check the logs for bulk execution submission
echo "Checking logs for bulk execution submission..."
sleep 5

# Look for bulk execution logs
if grep -q "Starting bulk execution submission for [2-9]" app.log; then
    echo "✅ SUCCESS: Found bulk execution submission in logs!"
    echo "Bulk execution submission is working correctly."
else
    echo "❌ FAILURE: No bulk execution submission found in logs."
    echo "The fix may not be working correctly."
fi

# Show relevant log lines
echo "Relevant log lines:"
grep -E "(bulk execution|batch.*execution|Split.*executions)" app.log | tail -10

# Cleanup
echo "Cleaning up..."
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "Test completed."