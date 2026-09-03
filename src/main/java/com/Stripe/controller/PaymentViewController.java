package com.Stripe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles the post-checkout redirect pages that Stripe returns the
 * customer to after completing or cancelling the hosted Checkout page.
 */
@Controller
@RequestMapping("/payment")
public class PaymentViewController {

    @GetMapping("/success")
    public String success() {
        return "success";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "cancel";
    }
}
