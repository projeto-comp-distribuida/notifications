# Notifications Service - Postman Collection

This directory contains Postman collections and environment files for testing the DistriSchool Notifications Service.

## Files

1. **`Notifications-Service.postman_collection.json`** - Complete Postman collection with all REST and WebSocket endpoints
2. **`Notifications-Service-Environment.postman_environment.json`** - Environment variables for different configurations

## Setup Instructions

### 1. Import Collection and Environment

1. Open Postman
2. Click **Import** button
3. Import both files:
   - `Notifications-Service.postman_collection.json`
   - `Notifications-Service-Environment.postman_environment.json`
4. Select the environment "DistriSchool Notifications Service - Environment"

### 2. Configure Environment Variables

Update the following variables based on your setup:

- **`gateway_url`**: API Gateway URL (default: `http://localhost:8080`)
- **`notifications_service_url`**: Direct notifications service URL (default: `http://localhost:8080`)
- **`gateway_ws_url`**: WebSocket URL via gateway (default: `ws://localhost:8080`)
- **`notifications_service_ws_url`**: Direct WebSocket URL (default: `ws://localhost:8080`)

### For Docker/Kubernetes deployments:

```json
{
  "gateway_url": "http://api-gateway:8080",
  "notifications_service_url": "http://notifications-service-dev:8080",
  "gateway_ws_url": "ws://api-gateway:8080",
  "notifications_service_ws_url": "ws://notifications-service-dev:8080"
}
```

## Available Endpoints

### REST API Endpoints

#### 1. Get All Notifications
- **Method**: `GET`
- **URL**: `{{gateway_url}}/api/v1/notifications`
- **Description**: Retrieves all notifications from the system
- **Response**: 
  ```json
  {
    "success": true,
    "message": "Found X notifications",
    "data": [
      {
        "id": "1",
        "type": "user.created",
        "title": "Novo Usuário Criado",
        "message": "Usuário João criado com sucesso",
        "timestamp": "2024-01-01T12:00:00",
        "read": false,
        "data": { ... }
      }
    ]
  }
  ```

#### 2. Mark Notification as Read
- **Method**: `PUT`
- **URL**: `{{gateway_url}}/api/v1/notifications/{id}/read`
- **Description**: Marks a specific notification as read
- **Path Parameters**: 
  - `id`: Notification ID
- **Response**:
  ```json
  {
    "success": true,
    "message": "Notification marked as read",
    "data": null
  }
  ```

#### 3. Health Check
- **Method**: `GET`
- **URL**: `{{gateway_url}}/api/v1/health`
- **Description**: Checks if the service is healthy

#### 4. Service Info
- **Method**: `GET`
- **URL**: `{{gateway_url}}/api/v1/health/info`
- **Description**: Gets detailed service information

### WebSocket Endpoints

#### 1. Connect via Gateway
- **URL**: `{{gateway_ws_url}}/ws/notifications/connect`
- **Protocol**: WebSocket (ws:// or wss://)
- **Description**: Connect through the API Gateway for WebSocket notifications

#### 2. Connect Direct to Service
- **URL**: `{{notifications_service_ws_url}}/ws/notifications`
- **Protocol**: WebSocket (ws:// or wss://)
- **Description**: Connect directly to the notifications service (bypassing gateway)

## Testing WebSocket in Postman

### Using Postman's WebSocket Support

1. **Create a WebSocket Request**:
   - Click **New** → **WebSocket Request**
   - Enter the WebSocket URL: `ws://localhost:8080/ws/notifications`
   - Click **Connect**

2. **Send Messages** (if needed):
   - Type a message in the message box
   - Click **Send**
   - Messages are typically JSON format

3. **Receive Messages**:
   - Once connected, you'll automatically receive notifications
   - Messages appear in the response panel

### WebSocket Message Format

**Received Notification**:
```json
{
  "type": "notification",
  "data": {
    "id": "1",
    "type": "user.created",
    "title": "Novo Usuário Criado",
    "message": "Usuário João criado com sucesso",
    "timestamp": "2024-01-01T12:00:00",
    "read": false,
    "data": {
      "userId": 123,
      "userEmail": "joao@example.com"
    }
  }
}
```

## Testing Notifications Flow

### 1. Create a Notification via Kafka Event

Notifications are automatically created when Kafka events are consumed. To test:

1. **Send a Kafka Event** (using Kafka tools or another service):
   ```json
   {
     "eventId": "test-event-123",
     "eventType": "user.created",
     "source": "auth-service",
     "version": "1.0",
     "timestamp": "2024-01-01T12:00:00",
     "data": {
       "userId": 123,
       "userName": "Test User",
       "userEmail": "test@example.com",
       "userRole": "STUDENT"
     }
   }
   ```

2. **Supported Event Types**:
   - `user.created` - Creates notification when user is created
   - `user.disabled` - Creates notification when user is disabled
   - `teacher.created` - Creates notification when teacher is created

### 2. Retrieve Notifications

1. Use **Get All Notifications** endpoint
2. Check the response for the new notification
3. Note the `id` of the notification

### 3. Mark as Read

1. Use **Mark Notification as Read** endpoint
2. Use the `id` from step 2
3. Verify the notification is marked as read

### 4. Test WebSocket (Real-time)

1. Connect to WebSocket endpoint
2. Keep connection open
3. Send a Kafka event (from step 1)
4. Receive the notification in real-time via WebSocket

## Troubleshooting

### WebSocket Connection Issues

1. **Check URL Format**: Ensure you're using `ws://` (not `http://`) for WebSocket URLs
2. **Check Port**: Ensure the service is running on the correct port
3. **Check Gateway**: If using gateway, ensure WebSocket route is configured
4. **CORS**: Check if CORS is properly configured for WebSocket connections

### REST API Issues

1. **404 Not Found**: 
   - Check if the service is running
   - Verify the gateway routes are configured correctly
   - Check the URL path matches the gateway configuration

2. **500 Internal Server Error**:
   - Check service logs
   - Verify database connection
   - Check Kafka connection

3. **Empty Response**:
   - Verify notifications exist in the database
   - Check if Kafka events are being consumed
   - Verify event types match supported types

## Environment Configurations

### Local Development
```json
{
  "gateway_url": "http://localhost:8080",
  "notifications_service_url": "http://localhost:8080",
  "gateway_ws_url": "ws://localhost:8080",
  "notifications_service_ws_url": "ws://localhost:8080"
}
```

### Docker Compose
```json
{
  "gateway_url": "http://localhost:8080",
  "notifications_service_url": "http://localhost:8080",
  "gateway_ws_url": "ws://localhost:8080",
  "notifications_service_ws_url": "ws://localhost:8080"
}
```

### Kubernetes
```json
{
  "gateway_url": "http://api-gateway-service:8080",
  "notifications_service_url": "http://notifications-service:8080",
  "gateway_ws_url": "ws://api-gateway-service:8080",
  "notifications_service_ws_url": "ws://notifications-service:8080"
}
```

## Additional Resources

- [Postman WebSocket Documentation](https://learning.postman.com/docs/sending-requests/websocket/)
- [Spring Cloud Gateway WebSocket](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#websocket-routing)
- [Kafka Event Documentation](./README.md)

## Notes

- WebSocket support requires Postman v8.0 or later
- For production, use `wss://` (secure WebSocket) instead of `ws://`
- Notifications are persisted in PostgreSQL database
- Kafka events are consumed automatically by the service
- The service filters events and only creates notifications for supported event types




