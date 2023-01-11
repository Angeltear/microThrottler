package com.angeltear.microthrottler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name="payments")
@Entity
public class PaymentRequest {
    @NotNull(message = "ClientID is mandatory!")
    @Positive(message = "ClientID must be greater than 0!")
    private long clientId;
    @NotNull(message = "Payment ID is mandatory!")
    @Positive(message = "Payment ID must be greater than 0!")
    @Id
    private long paymentId;
    @NotNull(message = "Payment Sum is mandatory!")
    @Positive(message = "Payment Sum must be greater than 0!")
    @Column(name = "amount")
    private double paymentSum;

}
