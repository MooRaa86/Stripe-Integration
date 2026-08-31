package com.Stripe.service;

import com.Stripe.entity.Order;
import com.Stripe.repository.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;

    public String createCheckoutSession(UUID orderId)
            throws StripeException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found")
                );

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
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
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPriceData(
                                                SessionCreateParams
                                                        .LineItem
                                                        .PriceData
                                                        .builder()
                                                        .setCurrency(order.getCurrency())
                                                        .setUnitAmount(
                                                                order
                                                                        .getAmount()
                                                                        .movePointRight(2)
                                                                        .longValue())
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
        return session.getUrl();
    }

}
