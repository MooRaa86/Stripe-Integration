package com.Stripe.service;

import com.Stripe.entity.Payment;
import com.Stripe.entity.PaymentStatus;
import com.Stripe.exception.PaymentNotFoundException;
import com.Stripe.repository.PaymentRepository;
import com.Stripe.repository.RefundRepository;
import com.Stripe.util.StripeAmountConverter;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Creates Stripe refunds on behalf of an API request. The Stripe Refund is the
 * source of truth; the local Payment status is only updated after Stripe
 * confirms the refund through the charge.refunded webhook.
 */
@Slf4j
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
                                new PaymentNotFoundException(paymentId));

        /*
         * A payment can only be refunded once it has been successfully
         * captured, or when it already holds a partial refund.
         */
        if (payment.getStatus() != PaymentStatus.SUCCEEDED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException(
                    "Payment in state " + payment.getStatus()
                            + " cannot be refunded");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero");
        }

        BigDecimal totalRefunded =
                refundRepository.getTotalRefunded(payment);

        BigDecimal remainingAmount =
                payment.getAmount().subtract(totalRefunded);

        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds the remaining refundable amount");
        }

        PaymentIntent paymentIntent =
                PaymentIntent.retrieve(
                        payment.getStripePaymentIntentId());

        if (paymentIntent.getLatestCharge() == null) {
            throw new IllegalStateException(
                    "No charge found for PaymentIntent "
                            + paymentIntent.getId());
        }

        RefundCreateParams params =
                RefundCreateParams.builder()
                        .setCharge(paymentIntent.getLatestCharge())
                        .setAmount(StripeAmountConverter.toSmallestUnits(amount))
                        .build();

        Refund refund = Refund.create(params);

        log.info("Stripe refund {} created for payment {}",
                refund.getId(), payment.getId());

        return refund.getId();
    }
}
