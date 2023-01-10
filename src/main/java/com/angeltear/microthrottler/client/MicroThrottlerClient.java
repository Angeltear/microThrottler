package com.angeltear.microthrottler.client;

import com.angeltear.microthrottler.Config.RedisConfig;
import com.angeltear.microthrottler.Model.PaymentRequest;
import com.angeltear.microthrottler.Serializer.PaymentRequestSerializer;
import com.google.common.util.concurrent.RateLimiter;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

@Component
@Slf4j

@NoArgsConstructor
public class MicroThrottlerClient {

    @Autowired
    private RedisConfig redisConfig;

    @Value("${redis.connection-timeout-seconds}")
    private long connectionTimeout;

    @Value("${rate-limit-per-second}")
    private long rateLimit;


    /* Using @EventListener(ApplicationReadyEvent.class) to tell Spring we need the method executed once the application has started.
     * In case of a timeout, the method will be recursively invoked.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initiateConsumer() {

        //Create amount of tokens, generated every second, based on a configuration parameter.
        RateLimiter rateLimiter = RateLimiter.create(rateLimit);

        RedisClient redisClient = redisConfig.getClient();
        log.info("Service started!");
        //Open a connection to the redis client submitting and accepting byte arrays instead of strings, since we're using custom complex objects.
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(new ByteArrayCodec());
        RedisCommands<byte[], byte[]> syncCommands = connection.sync();

        //Setting the timeout of the connection to retrieve an element. Changing the parameter to 0 in the application.properties file will remove the timeout and connection will be constant.
        connection.setTimeout(Duration.ofSeconds(connectionTimeout));
        log.info("Timeout set: " + connection.getTimeout().getSeconds() + " seconds.");

        PaymentRequestSerializer serializer = new PaymentRequestSerializer();

        while (true) {
            try {
                //Get total count of the queue
                Long totalQueueCount = syncCommands.llen("appQueue".getBytes());
                log.info("Current queue size " + totalQueueCount + ".");

                //Using a BRPOP command, so if the list is empty, we can block the connection in wait of a new element.
                KeyValue<byte[], byte[]> rr = syncCommands.brpop(0L, "appQueue".getBytes());
                PaymentRequest pr = serializer.decode(rr.getValue());
                log.info("Element obtained: " + pr.toString());
                //Element is successfully obtained, so remove an attempt from client's personal list of requests.
                syncCommands.rpop(Long.toString(pr.getClientId()).getBytes());

                /* Pass the element to the processor, along with the rateLimiter instance. We can also call the .acquire() method here to block and wait for
                 * available token before the call to processElement, but passing the limiter instance to get a token inside the method makes this usage easier
                 * to expand the functionality by passing the processElement to another thread for async processing.
                */
                processElement(rateLimiter, pr);

            }
            /* When no new elements appear to the queue before the timeout time is reached,
             * close the connection to avoid consuming the whole connection pool and recursively call this method to
             * restart the waiting for elements. This ensures that we're having 100% uptime on the execution.
             */ catch (RedisCommandTimeoutException redisConnectionException) {
                log.info("Timed out. Retrying!");
                connection.close();
                initiateConsumer();
            }
            /* When an error occurs, the connection is closing and the actual execution finishes. This can be further expanded
             * to make a call to a notification service (message broker, mail server, etc.) to notify the system users that
             * something wrong happened and caused an exception, so they can restart the service and look into the cause of the issue.
             */ catch (Exception e) {
                log.error(e.getMessage());
                connection.close();

            }

        }
    }

    /* In order to throttle the load on the database for a consistent performance, there are 2 algorithms that suit the purpose - Leaky Bucket and Token Bucket.
     * - Leaky bucket relies on the concept that inbound rate elements is not limited and processed with a fixed frequency. If the volume of inbound elements is too large,
     * subsequent requests are discarded (bucket is full). Such algorithm is implemented in the initiator Microservice in order to prevent abuse, DDoS, etc.
     * - Token bucket on the other hand relies on a "token" system that refills over time. This approach limits the average inflow rate (the popping of elements from the queue)
     * and processes the elements if there's enough tokens available in the bucket. This allows for a sudden spike in traffic. Several tokens can be obtained at the same time, but
     * if the max amount of tokens is reached, subsequent requests need to wait for a token to be released (processing of previous element has finished). This ensures steady average
     * flow to the database. Token Bucket is the approach that is selected for throttling the DB requests in this Microservice.
     *
     * For implementing the throttling approach, we're using Google's "Guava" open-source library. It generates tokens at a fixed rate (with or without warm-up period), has both
     * blocking (acquire) and non-blocking (tryAcquire) functionality. Its usage is also straight-forward.
     */
    public void processElement(RateLimiter rateLimiter, PaymentRequest paymentRequest){
        rateLimiter.acquire();
        log.info("Processing paymentRequest: " + paymentRequest.toString() + " at: " + new Date());
    }



}
