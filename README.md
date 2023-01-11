# MicroThrottler
Consumer microservice for [MicroInitiator](https://github.com/Angeltear/microInitiator). Its purpose is to consume the elements of a redis queue, populated by microInitiator and process them.

# Processing logic

1. Upon application startup, the service is spun up by initiateConsumer() method, annotated with @EventListener(ApplicationReadyEvent.class)
2. Rate limiter without warm-up time is created to ensure the smooth flow of the process with tokens amount, specified in a property.
3. Begin an infinite loop to pop the elements from a queue. Loop is escaped only if an exception occurs, different from redis timeout. If a timeout occurs, close the current connection to avoid leaks and recursively call the initiateConsumer() method.
4. Each element is popped from the main queue, alongside elements from client's personal queue, intended to limit the amount of simultaneous requests coming from the same client.
5. Each element in the main queue is sent to the service method for processing when a token is supplied from the limiter.
6. Processing happens in a transaction to ensure payment and balance are updated at the same time. In order to deal with potential data corruption, @Lock(LockModeType.PESSIMISTIC_WRITE) is enforced on the findById() method in the repository to lock the object for the duration of the current transaction.
7. After the element is processed, the loop continues with the next element if present. If the queue is empty, then it blocks it, until a new element is pushed.

# Startup configuration

1. For dev/demo purposes, all endpoints (Redis, Postgres, Application) run on localhost with their default ports.
2. Postgres database needs to be initialized with the necessary database/schema/tables. The files in SqlScripts folder can be used for this purpose.
3. Run the Redis server and Postgres instances on localhost.
4. Start the service.
