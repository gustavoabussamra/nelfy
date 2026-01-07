package com.fin.repository;

import com.fin.model.Referral;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferralCode(String referralCode);
    List<Referral> findByReferrer(User referrer);
    List<Referral> findByReferrerId(Long referrerId);
    Optional<Referral> findByReferred(User referred);
    Optional<Referral> findByReferredId(Long referredId);
    Long countByReferrerId(Long referrerId);
    Long countByReferrerIdAndRewardGivenTrue(Long referrerId);
}




