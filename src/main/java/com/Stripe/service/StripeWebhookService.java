package com.Stripe.service;

import com.Stripe.entity.Order;
import com.Stripe.entity.OrderStatus;
import com.Stripe.entity.Payment;
import com.Stripe.entity.PaymentStatus;
import com.Stripe.entity.Refund;
import com.Stripe.entity.StripeEvent;
import com.Stripe.exception.OrderNotFoundException;
import com.Stripe.exception.PaymentNotFoundException;
import com.Stripe.repository.OrderRepository;
import com.Stripe.repository.PaymentRepository;
import com.Stripe.repository.RefundRepository;
import com.Stripe.repository.StripeEventRepository;
import com.Stripe.util.StripeAmountConverter;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.RefundCollection;
import com.stripe.param.RefundListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Processes Stripe webhook events. Stripe does not guarantee event ordering,
 * so each handler must be able to run independently and be safe to retry.
 * <p>
 * Webhook retries: if processing throws, no event is marked processed and the
 * endpoint returns an error, prompting Stripe to retry the delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

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

        /*
         * The same Stripe event can be delivered more than once (retries or
         * duplicate delivery). Only process events that have not already been
         * handled successfully.
         */
        if (stripeEvent != null && stripeEvent.isProcessed()) {
            log.info("Skipping already processed event {}", event.getId());
            return;
        }

        if (stripeEvent == null) {
            /*
             * Persist the event before processing so that a concurrent retry
             * can see that delivery has begun. The whole transaction rolls
             * back if processing fails, so the event is left unprocessed.
             */
            stripeEvent = StripeEvent.builder()
                    .stripeEventId(event.getId())
                    .eventType(event.getType())
                    .processed(false)
                    .createdAt(Instant.now())
                    .build();
            stripeEventRepository.save(stripeEvent);
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> log.info("Ignoring unhandled event type {}", event.getType());
        }

        stripeEvent.setProcessed(true);
        stripeEventRepository.save(stripeEvent);
    }

    /**
     * Stores the Stripe resource identifiers on the local payment.
     * <p>
     * This handler intentionally updates only the identifier columns through a
     * targeted database query. If it saved the entire entity instead, a stale
     * copy of the Payment (for example one still holding PENDING) could
     * overwrite a newer status written by a concurrent webhook.
     */
    private void handleCheckoutSessionCompleted(Event event) {

        var session =
                deserialize(event,
                        com.stripe.model.checkout.Session.class,
                        "Checkout Session");

        String orderIdValue = getOrderIdFromMetadata(
                session.getMetadata());

        Order order = findOrder(orderIdValue);
        Payment payment = findPaymentForOrder(order);

        paymentRepository.updateStripeIds(
                payment.getId(),
                session.getId(),
                session.getPaymentIntent()
        );

        log.info("Checkout session stored for order {}", order.getId());
    }

    /**
     * Marks the payment and order as successful once Stripe confirms the
     * PaymentIntent succeeded.
     * <p>
     * orderId is stored in the PaymentIntent metadata so this handler does not
     * depend on checkout.session.completed having been processed first
     * (Stripe does not guarantee delivery order).
     */
    private void handlePaymentIntentSucceeded(Event event) {

        var paymentIntent =
                deserialize(event, com.stripe.model.PaymentIntent.class,
                        "PaymentIntent");

        String orderIdValue = getOrderIdFromMetadata(
                paymentIntent.getMetadata());

        Order order = findOrder(orderIdValue);
        Payment payment = findPaymentForOrder(order);

        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        order.setStatus(OrderStatus.PAID);

        paymentRepository.saveAndFlush(payment);
        orderRepository.saveAndFlush(order);

        log.info("Payment succeeded for order {}", order.getId());
    }

    /**
     * Marks the payment and order as failed once Stripe reports the
     * PaymentIntent could not be completed.
     */
    private void handlePaymentIntentFailed(Event event) {

        var paymentIntent =
                deserialize(event, com.stripe.model.PaymentIntent.class,
                        "PaymentIntent");

        String orderIdValue = getOrderIdFromMetadata(
                paymentIntent.getMetadata());

        Order order = findOrder(orderIdValue);
        Payment payment = findPaymentForOrder(order);

        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.FAILED);

        paymentRepository.save(payment);
        orderRepository.save(order);

        log.info("Payment failed for order {}", order.getId());
    }

    /**
     * Reconciles refunds created in Stripe with local Refund records and
     * updates the payment status to REFUNDED or PARTIALLY_REFUNDED.
     * <p>
     * The Charge event object does not contain the refunds as an expanded
     * collection, so the refunds are fetched from the Stripe API by charge id.
     * Processing is idempotent: a Stripe Refund is stored locally only once,
     * keyed by its unique Stripe refund id.
     */
    private void handleChargeRefunded(Event event) throws StripeException {

        var charge =
                deserialize(event, com.stripe.model.Charge.class,
                        "Charge");

        String paymentIntentId = charge.getPaymentIntent();
        if (paymentIntentId == null) {
            throw new IllegalStateException(
                    "PaymentIntent is missing from Charge event");
        }

        Payment payment =
                paymentRepository
                        .findByStripePaymentIntentId(paymentIntentId)
                        .orElseThrow(() ->
                                new PaymentNotFoundException(
                                        "Payment not found for PaymentIntent: "
                                                + paymentIntentId));

        RefundCollection refundCollection =
                com.stripe.model.Refund.list(RefundListParams.builder()
                        .setCharge(charge.getId())
                        .build());

        for (com.stripe.model.Refund stripeRefund
                : refundCollection.getData()) {

            if (refundRepository
                    .findByStripeRefundId(stripeRefund.getId())
                    .isPresent()) {
                log.info("Refund already processed {}",
                        stripeRefund.getId());
                continue;
            }

            Refund refund = Refund.builder()
                    .payment(payment)
                    .amount(StripeAmountConverter.fromSmallestUnits(
                            stripeRefund.getAmount()))
                    .currency(stripeRefund.getCurrency())
                    .stripeRefundId(stripeRefund.getId())
                    .createdAt(Instant.now())
                    .build();

            refundRepository.save(refund);
            log.info("Refund saved {}", stripeRefund.getId());
        }

        BigDecimal totalRefunded =
                refundRepository.getTotalRefunded(payment);

        /*
         * A refund request alone does not change the local status. The status
         * is updated only after Stripe confirms the refund through the
         * charge.refunded webhook.
         */
        if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        paymentRepository.save(payment);

        log.info("Payment {} refund status is now {}",
                payment.getId(), payment.getStatus());
    }

    private <T> T deserialize(Event event, Class<T> type, String label) {
        var optional =
                event.getDataObjectDeserializer().getObject();
        if (optional.isEmpty() || !type.isInstance(optional.get())) {
            throw new IllegalStateException(
                    "Could not deserialize " + label + " from event");
        }
        return type.cast(optional.get());
    }

    private String getOrderIdFromMetadata(
            java.util.Map<String, String> metadata
    ) {
        String orderIdValue = metadata.get("orderId");
        if (orderIdValue == null) {
            throw new IllegalStateException(
                    "orderId is missing from Stripe metadata");
        }
        return orderIdValue;
    }

    private Order findOrder(String orderIdValue) {
        UUID orderId;
        try {
            orderId = UUID.fromString(orderIdValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Invalid orderId in Stripe metadata: " + orderIdValue, e);
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Payment findPaymentForOrder(Order order) {
        return paymentRepository.findByOrder(order)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for order: " + order.getId()));
    }
}
