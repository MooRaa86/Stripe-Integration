package com.Stripe.repository;

import com.Stripe.entity.Payment;
import com.Stripe.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface RefundRepository
        extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByStripeRefundId(
            String stripeRefundId
    );
    List<Refund> findAllByPayment(Payment payment);

    @Query("""
    SELECT COALESCE(SUM(r.amount), 0)
    FROM Refund r
    WHERE r.payment = :payment
""")
    BigDecimal getTotalRefunded(
            @Param("payment") Payment payment
    );
}