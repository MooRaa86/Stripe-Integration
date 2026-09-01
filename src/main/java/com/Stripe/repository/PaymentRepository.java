package com.Stripe.repository;

import com.Stripe.entity.Order;
import com.Stripe.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByStripeCheckoutSessionId(
            String stripeCheckoutSessionId
    );

    Optional<Payment> findByStripePaymentIntentId(
            String stripePaymentIntentId
    );

    Optional<Payment> findByOrder(Order order);

    @Modifying
    @Query("""
    update Payment p
    set p.stripeCheckoutSessionId = :sessionId,
        p.stripePaymentIntentId = :paymentIntentId
    where p.id = :paymentId
""")
    int updateStripeIds(
            @Param("paymentId") UUID paymentId,
            @Param("sessionId") String sessionId,
            @Param("paymentIntentId") String paymentIntentId
    );
}