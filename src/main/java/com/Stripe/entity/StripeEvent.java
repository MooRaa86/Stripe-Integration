package com.Stripe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stripe_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stripe_event_id",
                        columnNames = "stripe_event_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "stripe_event_id",
            nullable = false,
            unique = true
    )
    private String stripeEventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private Instant createdAt;
}