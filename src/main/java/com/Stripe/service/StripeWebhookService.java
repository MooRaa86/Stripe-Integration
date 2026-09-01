package com.Stripe.service;

import com.Stripe.entity.*;
import com.Stripe.repository.OrderRepository;
import com.Stripe.repository.PaymentRepository;
import com.Stripe.repository.RefundRepository;
import com.Stripe.repository.StripeEventRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    //stripe listen --forward-to localhost:8080/api/webhooks/stripe

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StripeEventRepository stripeEventRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public void handleEvent(Event event) throws StripeException {

        StripeEvent stripeEvent =
                stripeEventRepository
                        .findByStripeEventId(event.getId())
                        .orElse(null);

        // Event was successfully processed before
        if (stripeEvent != null && stripeEvent.isProcessed()) {

            System.out.println(
                    "Event already processed: "
                            + event.getId()
            );

            return;
        }

        // Event exists but previous processing failed
        if (stripeEvent == null) {

            stripeEvent = StripeEvent.builder()
                    .stripeEventId(event.getId())
                    .eventType(event.getType())
                    .processed(false)
                    .createdAt(Instant.now())
                    .build();

            stripeEventRepository.save(stripeEvent);
        }

        switch (event.getType()) {

            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;

            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;

            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;

            case "charge.refunded":
                handleChargeRefunded(event);
                break;

            default:
                System.out.println(
                        "Unhandled event: " + event.getType()
                );
        }

        stripeEvent.setProcessed(true);
        stripeEventRepository.save(stripeEvent);
    }

    private void handleCheckoutSessionCompleted(
            Event event
    ) {

        var optionalSession =
                event.getDataObjectDeserializer()
                        .getObject();

        if (optionalSession.isEmpty()) {
            throw new IllegalStateException(
                    "Could not deserialize Checkout Session"
            );
        }

        var session =
                (com.stripe.model.checkout.Session)
                        optionalSession.get();

        String orderIdValue =
                session.getMetadata().get("orderId");

        if (orderIdValue == null) {
            throw new IllegalStateException(
                    "orderId is missing from Stripe metadata"
            );
        }

        UUID orderId;

        try {
            orderId = UUID.fromString(orderIdValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Invalid orderId in Stripe metadata",
                    e
            );
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found: " + orderId
                        )
                );

        Payment payment =
                paymentRepository.findByOrder(order)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );

        paymentRepository.updateStripeIds(
                payment.getId(),
                session.getId(),
                session.getPaymentIntent()
        );

        System.out.println(
                "Checkout session processed for order: "
                        + orderId
        );
    }

    private void handlePaymentIntentSucceeded(Event event) {

        var optionalPaymentIntent =
                event.getDataObjectDeserializer()
                        .getObject();

        if (optionalPaymentIntent.isEmpty()) {
            throw new IllegalStateException(
                    "Could not deserialize PaymentIntent"
            );
        }

        var paymentIntent =
                (com.stripe.model.PaymentIntent)
                        optionalPaymentIntent.get();

        String paymentIntentId =
                paymentIntent.getId();

        String orderIdValue =
                paymentIntent.getMetadata().get("orderId");

        if (orderIdValue == null) {
            throw new IllegalStateException(
                    "orderId is missing from PaymentIntent metadata"
            );
        }

        UUID orderId = UUID.fromString(orderIdValue);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found: " + orderId
                        )
                );

        Payment payment =
                paymentRepository.findByOrder(order)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );

        payment.setStripePaymentIntentId(
                paymentIntentId
        );

        payment.setStatus(
                PaymentStatus.SUCCEEDED
        );

        order.setStatus(
                OrderStatus.PAID
        );

        paymentRepository.saveAndFlush(payment);
        orderRepository.saveAndFlush(order);

        System.out.println(
                "Payment succeeded for order: "
                        + orderId
        );

    }

    private void handlePaymentIntentFailed(Event event) {

        var optionalPaymentIntent =
                event.getDataObjectDeserializer()
                        .getObject();

        if (optionalPaymentIntent.isEmpty()) {
            throw new IllegalStateException(
                    "Could not deserialize PaymentIntent"
            );
        }

        var paymentIntent =
                (com.stripe.model.PaymentIntent)
                        optionalPaymentIntent.get();

        String paymentIntentId =
                paymentIntent.getId();

        String orderIdValue =
                paymentIntent.getMetadata().get("orderId");

        if (orderIdValue == null) {
            throw new IllegalStateException(
                    "orderId is missing from PaymentIntent metadata"
            );
        }

        UUID orderId = UUID.fromString(orderIdValue);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found: " + orderId
                        )
                );

        Payment payment =
                paymentRepository.findByOrder(order)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment not found for order: "
                                                + orderId
                                )
                        );

        payment.setStripePaymentIntentId(
                paymentIntentId
        );

        payment.setStatus(
                PaymentStatus.FAILED
        );

        order.setStatus(
                OrderStatus.FAILED
        );

        paymentRepository.save(payment);
        orderRepository.save(order);

        System.out.println(
                "Payment failed for order: "
                        + orderId
        );
    }

    private void handleChargeRefunded(Event event) throws StripeException {

        var optionalCharge =
                event.getDataObjectDeserializer()
                        .getObject();

        if (optionalCharge.isEmpty()) {
            throw new IllegalStateException(
                    "Could not deserialize Charge"
            );
        }

        var charge =
                (com.stripe.model.Charge)
                        optionalCharge.get();

        String paymentIntentId =
                charge.getPaymentIntent();

        if (paymentIntentId == null) {
            throw new IllegalStateException(
                    "PaymentIntent is missing from Charge"
            );
        }

        Payment payment =
                paymentRepository
                        .findByStripePaymentIntentId(
                                paymentIntentId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment not found for PaymentIntent: "
                                                + paymentIntentId
                                )
                        );

        // Get refunds directly from Stripe
        com.stripe.model.RefundCollection refundCollection =
                com.stripe.model.Refund.list(
                        com.stripe.param.RefundListParams.builder()
                                .setCharge(charge.getId())
                                .build()
                );

        for (com.stripe.model.Refund stripeRefund
                : refundCollection.getData()) {

            String stripeRefundId =
                    stripeRefund.getId();

            // Idempotency check
            if (refundRepository
                    .findByStripeRefundId(stripeRefundId)
                    .isPresent()) {

                System.out.println(
                        "Refund already processed: "
                                + stripeRefundId
                );

                continue;
            }

            Refund refund =
                    Refund.builder()
                            .payment(payment)
                            .amount(
                                    BigDecimal
                                            .valueOf(
                                                    stripeRefund.getAmount()
                                            )
                                            .movePointLeft(2)
                            )
                            .currency(
                                    stripeRefund.getCurrency()
                            )
                            .stripeRefundId(
                                    stripeRefundId
                            )
                            .createdAt(Instant.now())
                            .build();

            refundRepository.save(refund);

            System.out.println(
                    "Refund saved: "
                            + stripeRefundId
            );
        }

        // Calculate total refunded
        BigDecimal totalRefunded =
                refundRepository.getTotalRefunded(payment);

        System.out.println(
                "Total refunded: "
                        + totalRefunded
        );

        // Update payment status
        if (totalRefunded.compareTo(
                payment.getAmount()
        ) >= 0) {

            payment.setStatus(
                    PaymentStatus.REFUNDED
            );

        } else {

            payment.setStatus(
                    PaymentStatus.PARTIALLY_REFUNDED
            );
        }

        paymentRepository.save(payment);

        System.out.println(
                "Payment refund status: "
                        + payment.getStatus()
        );
    }

}