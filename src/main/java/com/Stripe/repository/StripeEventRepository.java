package com.Stripe.repository;

import com.Stripe.entity.StripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StripeEventRepository
        extends JpaRepository<StripeEvent, UUID> {

    Optional<StripeEvent> findByStripeEventId(
            String stripeEventId
    );
}