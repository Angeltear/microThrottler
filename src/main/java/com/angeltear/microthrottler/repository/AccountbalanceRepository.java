package com.angeltear.microthrottler.repository;

import com.angeltear.microthrottler.model.Accountbalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Repository
public interface AccountbalanceRepository extends JpaRepository<Accountbalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Accountbalance> findById(Long customerId);




}