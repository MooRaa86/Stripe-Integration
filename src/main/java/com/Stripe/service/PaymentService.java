package com.Stripe.service;

import com.Stripe.entity.Order;
import com.Stripe.entity.Payment;
import com.Stripe.entity.PaymentStatus;
import com.Stripe.repository.OrderRepository;
import com.Stripe.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public String createCheckoutSession(UUID orderId)
            throws StripeException {

        // 1. Get Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found")
                );

        // 2. Create Payment
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // 3. Create Stripe Checkout Session
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(
                                SessionCreateParams.Mode.PAYMENT
                        )
                        .setSuccessUrl(
                                "http://localhost:8080/payment/success"
                        )
                        .setCancelUrl(
                                "http://localhost:8080/payment/cancel"
                        )
                        .putMetadata(
                                "orderId",
                                order.getId().toString()
                        )
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .putMetadata(
                                                "orderId",
                                                order.getId().toString()
                                        )
                                        .build()
                        )
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPriceData(
                                                SessionCreateParams
                                                        .LineItem
                                                        .PriceData
                                                        .builder()
                                                        .setCurrency(
                                                                order.getCurrency()
                                                        )
                                                        .setUnitAmount(
                                                                order.getAmount()
                                                                        .movePointRight(2)
                                                                        .longValue()
                                                        )
                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()
                                                                        .setName("Spring Boot Course")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .setQuantity(1L)
                                        .build()
                        )
                        .build();

        Session session = Session.create(params);

        // 4. Save Stripe Session ID
        payment.setStripeCheckoutSessionId(
                session.getId()
        );

        paymentRepository.save(payment);

        return session.getUrl();
    }

}
