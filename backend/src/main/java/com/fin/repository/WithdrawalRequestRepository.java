package com.fin.repository;

import com.fin.model.User;
import com.fin.model.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserOrderByCreatedAtDesc(User user);
    Page<WithdrawalRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalRequest.WithdrawalStatus status, Pageable pageable);
    Page<WithdrawalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<WithdrawalRequest> findFirstByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime date);
    List<WithdrawalRequest> findByUserAndStatusIn(User user, List<WithdrawalRequest.WithdrawalStatus> statuses);
}



