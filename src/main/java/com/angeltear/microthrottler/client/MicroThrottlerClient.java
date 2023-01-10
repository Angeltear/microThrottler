package com.angeltear.microthrottler.client;

import com.angeltear.microthrottler.Config.RedisConfig;
import com.angeltear.microthrottler.Model.PaymentRequest;
import com.angeltear.microthrottler.Serializer.PaymentRequestSerializer;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class MicroThrottlerClient {

    @Autowired
    private RedisConfig redisConfig;

    @Value("${redis.connection-timeout-seconds}")
    private long connectionTimeout;

    /* Using @PostConstruct to tell Spring we need the method executed once the context and properties have been initialized.
     *  If the method is called from the main method of the application, it will fail, because context loads later and Spring
     *  can not inject the RedisConfig instance. In any case, PostConstruct works great, because we need to call this method once
     *  the service is up and in case of a timeout, the method will be recursively invoked.
     */
    @PostConstruct
    public void initiateConsumer() {
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
                Thread.sleep(2000); //For test purposes
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


}
