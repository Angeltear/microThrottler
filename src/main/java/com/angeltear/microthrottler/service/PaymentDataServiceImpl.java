package com.angeltear.microthrottler.service;

import com.angeltear.microthrottler.model.PaymentRequest;
import com.angeltear.microthrottler.model.Accountbalance;
import com.angeltear.microthrottler.repository.AccountbalanceRepository;
import com.angeltear.microthrottler.repository.PaymentRequestRepository;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.support.QuerydslJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentDataServiceImpl implements PaymentDataService{

    private final PaymentRequestRepository paymentRepository;
    private final AccountbalanceRepository accountbalanceRepository;


    /* Add Transactional annotation to ensure both the insertion of the new payment and the balance update happen together (in the same transaction).
     * This can also be achieved by using PL/SQL procedure (and in the case of postgres - Function).
    */
    @Transactional
    public PaymentRequest savePayment(PaymentRequest payment) {
        PaymentRequest pr = paymentRepository.findById(payment.getPaymentId()).orElse(null);
        if(pr!=null){
            log.info("Payment ID already exist: " + payment.getPaymentId() + ". No action to be taken.");
            return payment;
        }
        PaymentRequest paymentOutput = paymentRepository.save(payment);

        log.info("Saved paymentRequest: " + paymentOutput);

        /* When we obtain the current balance, it's imperative to lock the database row, containing the object to avoid other threads changing the balance while we're processing the record.
         * This ensures the data consistency. For this reason, the findById method in the account balance repository is annotated with @Lock and locking mode PESIMISTIC_WRITE. This is
         * equivalent to the SQL command FOR UPDATE which locks all rows, affected by the query. If another transaction try to access the object, it will wait for this one to finish, before
         * it can access the object.
         */
        Accountbalance balance = accountbalanceRepository.findById(paymentOutput.getClientId()).orElse(new Accountbalance(payment.getClientId(),0));

        log.info("<" + payment.getPaymentId() + ">" + " Current balance for client: " + balance.getClientId() + " is " + balance.getBalance());

        balance.setBalance(balance.getBalance() + paymentOutput.getPaymentSum());
        accountbalanceRepository.save(balance);
        log.info("<" + payment.getPaymentId() + ">" + " Updated balance before flush: " + balance.getClientId() + " is " + balance.getBalance());

        /* Uncomment this block in order to observe the lock wait in testTransactionLock test and in DB (system table pg_locks)*/
        /*

        try {
            Thread.sleep(3000);
        }
        catch (Exception e){
            log.error("Error occurred: " + e.getMessage());
        }
        */

        accountbalanceRepository.flush();

        log.info("<" + payment.getPaymentId() + ">" + " Balance for client: " + balance.getClientId() + " updated to " + balance.getBalance());

        return paymentOutput;
    }

}
