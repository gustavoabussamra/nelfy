package com.fin.repository;

import com.fin.model.Referral;
import com.fin.model.ReferralCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralCommissionRepository extends JpaRepository<ReferralCommission, Long> {
    List<ReferralCommission> findByReferral(Referral referral);
    List<ReferralCommission> findByReferralOrderByPaymentYearDescPaymentMonthDesc(Referral referral);
    Optional<ReferralCommission> findByReferralAndPaymentYearAndPaymentMonth(Referral referral, Integer year, Integer month);
    List<ReferralCommission> findByReferralReferrerId(Long referrerId);
    List<ReferralCommission> findByReferralIdIn(List<Long> referralIds);
}



