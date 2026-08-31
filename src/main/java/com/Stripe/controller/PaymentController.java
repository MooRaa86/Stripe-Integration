package com.Stripe.controller;

import com.Stripe.dto.CheckoutResponse;
import com.Stripe.dto.CreateCheckoutRequest;
import com.Stripe.service.PaymentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse createCheckout(
            @Valid @RequestBody CreateCheckoutRequest request
    ) throws StripeException {

        String checkoutUrl =
                paymentService.createCheckoutSession(
                        request.orderId()
                );

        return new CheckoutResponse(checkoutUrl);
    }
}