#!/bin/bash

echo "Testing Metrics Endpoint Implementation"
echo "======================================"

# Run the unit tests
echo "1. Running unit tests..."
./gradlew test --tests MetricsControllerTest --quiet
if [ $? -eq 0 ]; then
    echo "✅ Unit tests passed"
else
    echo "❌ Unit tests failed"
    exit 1
fi

# Run the integration tests
echo "2. Running integration tests..."
./gradlew test --tests MetricsEndpointIntegrationTest --quiet
if [ $? -eq 0 ]; then
    echo "✅ Integration tests passed"
else
    echo "❌ Integration tests failed"
    exit 1
fi

# Verify the application can compile and build
echo "3. Verifying application builds..."
./gradlew build --quiet
if [ $? -eq 0 ]; then
    echo "✅ Application builds successfully"
else
    echo "❌ Application build failed"
    exit 1
fi

echo ""
echo "✅ All tests passed! Metrics endpoint implementation is complete."
echo ""
echo "Task 1 Implementation Summary:"
echo "- ✅ Implemented /metrics endpoint that returns Prometheus format"
echo "- ✅ Added basic health check to verify endpoint accessibility (/metrics/health)"
echo "- ✅ Test endpoint returns valid response format"
echo "- ✅ Requirements 4.1, 4.2, 4.3 satisfied"
echo ""
echo "The metrics endpoint is ready for early validation!"