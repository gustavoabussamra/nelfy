package com.fin.repository;

import com.fin.model.Account;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    List<Account> findByUserId(Long userId);
    List<Account> findByUserIdAndIsActiveTrue(Long userId);
    List<Account> findByUserIdAndType(Long userId, Account.AccountType type);
}




