package com.Stripe.service;

import com.Stripe.entity.Order;
import com.Stripe.entity.Payment;
import com.Stripe.entity.PaymentStatus;
import com.Stripe.exception.OrderNotFoundException;
import com.Stripe.repository.OrderRepository;
import com.Stripe.repository.PaymentRepository;
import com.Stripe.util.StripeAmountConverter;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public String createCheckoutSession(UUID orderId)
            throws StripeException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        /*
         * orderId is stored as metadata on both the Checkout Session and the
         * PaymentIntent. Stripe does not guarantee webhook delivery order, so
         * storing it on the PaymentIntent lets payment_intent.succeeded be
         * processed even if it arrives before checkout.session.completed.
         */
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(
                                "http://localhost:8080/payment/success")
                        .setCancelUrl(
                                "http://localhost:8080/payment/cancel")
                        .putMetadata("orderId", order.getId().toString())
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata(
                                                "orderId",
                                                order.getId().toString())
                                        .build())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPriceData(
                                                SessionCreateParams
                                                        .LineItem
                                                        .PriceData
                                                        .builder()
                                                        .setCurrency(
                                                                order.getCurrency())
                                                        .setUnitAmount(
                                                                StripeAmountConverter
                                                                        .toSmallestUnits(
                                                                                order.getAmount()))
                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()
                                                                        .setName(
                                                                                "Spring Boot Course")
                                                                        .build())
                                                        .build())
                                        .setQuantity(1L)
                                        .build())
                        .build();

        Session session = Session.create(params);

        payment.setStripeCheckoutSessionId(session.getId());
        paymentRepository.save(payment);

        log.info("Checkout session {} created for order {}",
                session.getId(), order.getId());

        return session.getUrl();
    }
}
