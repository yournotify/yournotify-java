# Yournotify Java SDK

Java 17+ SDK for server-side integrations. It includes lowercase engagement channel accessors, Contact and Lists management, the shared event gateway, safe retries, idempotency, signed webhook verification and structured API errors. React Native/Expo applications use `@yournotify/react-native` so permanent API keys never enter a mobile bundle.

```java
Yournotify yn = new Yournotify(System.getenv("YOURNOTIFY_API_KEY"));
yn.track(Map.of("event", "order.completed", "external_id", "contact_123", "idempotency_key", "order_123"));
yn.voice().send(Map.of("name", "Order update", "sender", "Feedcover", "lists", List.of("+2348012345678")));
```
