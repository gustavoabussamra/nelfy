package com.fin.repository;

import com.fin.model.TransactionAttachment;
import com.fin.model.Transaction;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachment, Long> {
    List<TransactionAttachment> findByTransaction(Transaction transaction);
    List<TransactionAttachment> findByTransactionId(Long transactionId);
    List<TransactionAttachment> findByUser(User user);
    List<TransactionAttachment> findByUserId(Long userId);
    void deleteByTransactionId(Long transactionId);
}




