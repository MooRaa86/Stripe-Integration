package com.Stripe.service;

import com.Stripe.entity.Payment;
import com.Stripe.entity.PaymentStatus;
import com.Stripe.repository.PaymentRepository;
import com.Stripe.repository.RefundRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public String refundPayment(
            UUID paymentId,
            BigDecimal amount
    ) throws StripeException {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found"
                                )
                        );

        // Payment must be refundable
        if (payment.getStatus() != PaymentStatus.SUCCEEDED
                && payment.getStatus()
                != PaymentStatus.PARTIALLY_REFUNDED) {

            throw new IllegalStateException(
                    "Payment cannot be refunded"
            );
        }

        // Refund amount must be positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero"
            );
        }

        // Calculate already refunded amount
        BigDecimal totalRefunded =
                refundRepository.getTotalRefunded(payment);

        // Calculate remaining amount
        BigDecimal remainingAmount =
                payment.getAmount()
                        .subtract(totalRefunded);

        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds remaining amount"
            );
        }

        // Get Stripe PaymentIntent
        PaymentIntent paymentIntent =
                PaymentIntent.retrieve(
                        payment.getStripePaymentIntentId()
                );

        String chargeId =
                paymentIntent.getLatestCharge();

        if (chargeId == null) {
            throw new IllegalStateException(
                    "No charge found for PaymentIntent"
            );
        }

        // Create Stripe Refund
        RefundCreateParams params =
                RefundCreateParams.builder()
                        .setCharge(chargeId)
                        .setAmount(
                                amount
                                        .movePointRight(2)
                                        .longValue()
                        )
                        .build();

        Refund refund = Refund.create(params);

        return refund.getId();
    }
}