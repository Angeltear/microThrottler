package com.angeltear.microthrottler;

import com.angeltear.microthrottler.model.PaymentRequest;
import com.angeltear.microthrottler.service.PaymentDataService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
@Slf4j
class MicroThrottlerApplicationTests {

    @Autowired
    private PaymentDataService paymentDataService;

    @Test
    void contextLoads() {
    }

    //Before running this test, comment out line 47 in MicroThrottlerClient
    @Test
    void testTransactionLock() {

        /* This test only fails when there are no rows for the customer in AccountBalance table. Hibernate first performs the check and then the lock/wait is
         * performed on the findById method. When there already is data for a particular customer (only 1 row is expected, since client_Id is primary key),
         * everything works as expected with the update, but when there's no data present, Hibernate marks the transaction as insert, instead of update and then
         * begin the wait process. When the first thread finish the transaction and release the lock, the second one starts, but it's still marked as insert and
         * the transaction can not complete, because it fails the validation check. This problem can be solved in one of two ways:
         * 1. Initial insert in AccountBalance is performed in another component (microservice that handles client registration, etc.), so when this service is working,
         * it already has data and just perform updates, in compliance with the locks.
         * 2. Transfer the lock logic and additional checks in PL/SQL Procedure or PL/pgSQL Function and handle the insert/update process inside the db.
         *
         * P.S. This failure is an extreme corner case. For it to happen, 2 payments for the same client need to happen simultaneously in the time frame between start and finish of a single transaction.
         */

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<PaymentRequest> future = executorService.submit(() -> paymentDataService.savePayment(new PaymentRequest(1, 2, 1)));

        /* This block can be uncommented to compensate the problem, explained above. Local tests show that 300ms are enough time for the first transaction to complete and the 2nd is processed normally (with update).

        try {
            Thread.sleep(300);
        }
        catch (Exception e){
            log.error("Error occurred: " + e.getMessage());
        }
        */

        Future<PaymentRequest> future2 = executorService.submit(() -> paymentDataService.savePayment(new PaymentRequest(1, 1, 1)));

        try {
            future.get();
            future2.get();
        } catch (Exception e) {
            log.error("ERROR: " + e.getMessage());
        }

    }

}
