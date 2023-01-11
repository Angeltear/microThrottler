package com.angeltear.microthrottler.service;

import com.angeltear.microthrottler.model.PaymentRequest;
import org.springframework.stereotype.Service;

@Service
public interface PaymentDataService {

    PaymentRequest savePayment(PaymentRequest payment);
}
