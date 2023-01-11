package com.angeltear.microthrottler.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
public class Accountbalance {
    @Id
    private long clientId;
    private double balance;
}
