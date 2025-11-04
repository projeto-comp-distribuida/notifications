#!/bin/bash
# Script to manually send a Kafka event to test the notification system

set -e

KAFKA_CONTAINER="kafka"
TOPIC="distrischool.auth.user.created"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S")

# Generate test event
TEST_EVENT=$(cat <<EOF
{
  "eventId": "test-event-$(date +%s)",
  "eventType": "user.created",
  "source": "auth-service",
  "version": "1.0",
  "timestamp": "$TIMESTAMP",
  "data": {
    "userId": "test-user-$(date +%s)",
    "userName": "Test User Manual",
    "userEmail": "testmanual_$(date +%s)@example.com",
    "userRole": "STUDENT"
  }
}
EOF
)

echo "🚀 Sending test event to Kafka"
echo "=================================="
echo "Topic: $TOPIC"
echo "Event:"
echo "$TEST_EVENT" | python3 -m json.tool
echo ""

# Send event to Kafka
if docker exec $KAFKA_CONTAINER kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic $TOPIC <<< "$TEST_EVENT" 2>/dev/null; then
    echo "✅ Event sent successfully to Kafka topic: $TOPIC"
    echo ""
    echo "📊 Checking notifications..."
    sleep 2
    
    # Check notifications via REST API
    NOTIFICATIONS=$(curl -s "http://localhost:8080/api/v1/notifications" 2>/dev/null || echo "[]")
    if [[ "$NOTIFICATIONS" == *"Test User Manual"* ]] || [[ "$NOTIFICATIONS" == *"user.created"* ]]; then
        echo "✅ Notification created successfully!"
        echo ""
        echo "Notifications:"
        echo "$NOTIFICATIONS" | python3 -m json.tool 2>/dev/null || echo "$NOTIFICATIONS"
    else
        echo "⚠️  Notification not found yet. Check:"
        echo "   1. Notification service logs: docker logs <notifications-container>"
        echo "   2. Notification service is running: curl http://localhost:8080/actuator/health"
        echo "   3. Kafka consumer is working"
    fi
else
    echo "❌ Failed to send event to Kafka"
    echo "Make sure:"
    echo "   1. Kafka container is running: docker ps | grep kafka"
    echo "   2. Topic exists: docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:9092 --list"
    exit 1
fi

echo ""
echo "🔌 WebSocket Test"
echo "================="
echo "To test WebSocket, connect to: ws://localhost:8080/ws/notifications"
echo ""
echo "Example using wscat (if installed):"
echo "  npm install -g wscat"
echo "  wscat -c ws://localhost:8080/ws/notifications"
echo ""
echo "Or use the browser console:"
echo "  const ws = new WebSocket('ws://localhost:8080/ws/notifications');"
echo "  ws.onmessage = (e) => console.log('Received:', JSON.parse(e.data));"
echo "  ws.onopen = () => ws.send(JSON.stringify({type: 'subscribe'}));"


