#!/bin/bash
# Script to test Kafka and WebSocket integration
# Creates a user and monitors Kafka events and WebSocket notifications

set -e

AUTH_SERVICE_URL="http://localhost:8082"
NOTIFICATIONS_SERVICE_URL="http://localhost:8080"
WS_URL="ws://localhost:8080/ws/notifications"

echo "🔍 Testing Kafka and WebSocket Integration"
echo "=========================================="
echo ""

# Generate random email for testing
TIMESTAMP=$(date +%s)
TEST_EMAIL="testuser_${TIMESTAMP}@example.com"
TEST_PASSWORD="TestPassword123!"
TEST_NAME="Test User ${TIMESTAMP}"

echo "📝 Test User Details:"
echo "   Email: $TEST_EMAIL"
echo "   Name: $TEST_NAME"
echo ""

# Step 1: Check if auth service is running
echo "1️⃣  Checking auth service health..."
AUTH_HEALTH=$(curl -s "$AUTH_SERVICE_URL/actuator/health" || echo "ERROR")
if [[ "$AUTH_HEALTH" == *"UP"* ]]; then
    echo "   ✅ Auth service is running"
else
    echo "   ❌ Auth service is not responding"
    exit 1
fi

# Step 2: Check if notifications service is running
echo ""
echo "2️⃣  Checking notifications service health..."
NOTIF_HEALTH=$(curl -s "$NOTIFICATIONS_SERVICE_URL/actuator/health" || echo "ERROR")
if [[ "$NOTIF_HEALTH" == *"UP"* ]]; then
    echo "   ✅ Notifications service is running"
else
    echo "   ⚠️  Notifications service is not responding (may not be running)"
fi

# Step 3: Try to create a user via auth service
echo ""
echo "3️⃣  Creating user via auth service..."
echo "   POST $AUTH_SERVICE_URL/api/auth/register"
echo "   Body: {\"email\": \"$TEST_EMAIL\", \"password\": \"$TEST_PASSWORD\", \"name\": \"$TEST_NAME\"}"

# Try common endpoints
ENDPOINTS=(
    "/api/auth/register"
    "/api/users/register"
    "/api/auth/signup"
    "/users/register"
    "/auth/register"
)

USER_CREATED=false
for endpoint in "${ENDPOINTS[@]}"; do
    echo "   Trying: $endpoint"
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_SERVICE_URL$endpoint" \
        -H "Content-Type: application/json" \
        -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"$TEST_PASSWORD\", \"name\": \"$TEST_NAME\"}" 2>&1)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [[ "$HTTP_CODE" == "200" ]] || [[ "$HTTP_CODE" == "201" ]] || [[ "$HTTP_CODE" == "204" ]]; then
        echo "   ✅ User created successfully! (HTTP $HTTP_CODE)"
        echo "   Response: $BODY"
        USER_CREATED=true
        break
    elif [[ "$HTTP_CODE" != "404" ]] && [[ "$HTTP_CODE" != "000" ]]; then
        echo "   ⚠️  Got HTTP $HTTP_CODE: $BODY"
    fi
done

if [ "$USER_CREATED" = false ]; then
    echo "   ⚠️  Could not find the correct endpoint. Please check the auth service API documentation."
    echo "   You may need to manually create a user to trigger the Kafka event."
fi

# Step 4: Check Kafka topics
echo ""
echo "4️⃣  Checking Kafka topics..."
if command -v docker &> /dev/null; then
    echo "   Checking Kafka container..."
    KAFKA_CONTAINER=$(docker ps | grep kafka | awk '{print $1}' | head -1)
    if [ -n "$KAFKA_CONTAINER" ]; then
        echo "   ✅ Kafka container is running: $KAFKA_CONTAINER"
        echo "   Listing topics..."
        docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null || echo "   ⚠️  Could not list topics"
    else
        echo "   ⚠️  Kafka container not found"
    fi
else
    echo "   ⚠️  Docker not available to check Kafka"
fi

# Step 5: Check notifications via REST API
echo ""
echo "5️⃣  Checking notifications via REST API..."
sleep 2  # Wait a bit for the event to be processed
NOTIFICATIONS=$(curl -s "$NOTIFICATIONS_SERVICE_URL/api/v1/notifications" || echo "[]")
if [[ "$NOTIFICATIONS" == *"testuser"* ]] || [[ "$NOTIFICATIONS" == *"user.created"* ]]; then
    echo "   ✅ Found notifications related to user creation!"
    echo "$NOTIFICATIONS" | python3 -m json.tool 2>/dev/null || echo "$NOTIFICATIONS"
else
    echo "   ⚠️  No notifications found yet (this is normal if the endpoint doesn't exist or event wasn't published)"
    echo "   Response: $NOTIFICATIONS"
fi

# Step 6: WebSocket connection info
echo ""
echo "6️⃣  WebSocket Information:"
echo "   URL: $WS_URL"
echo "   To test WebSocket manually, you can use:"
echo "   - wscat: wscat -c $WS_URL"
echo "   - Postman: Create a WebSocket request to $WS_URL"
echo "   - Browser: Use JavaScript WebSocket API"
echo ""
echo "   Example JavaScript code:"
echo "   const ws = new WebSocket('$WS_URL');"
echo "   ws.onmessage = (event) => console.log('Received:', JSON.parse(event.data));"
echo "   ws.onopen = () => ws.send(JSON.stringify({type: 'subscribe'}));"

echo ""
echo "✅ Test completed!"
echo ""
echo "Next steps:"
echo "1. Check Kafka UI at http://localhost:8090 to see if events were published"
echo "2. Connect to WebSocket at $WS_URL to receive real-time notifications"
echo "3. Check notification service logs: docker logs <notifications-container>"
echo "4. Check auth service logs: docker logs microservice-auth-dev-2"

