package com.Stripe.service;

import com.Stripe.dto.CreateOrderRequest;
import com.Stripe.entity.Order;
import com.Stripe.entity.OrderStatus;
import com.Stripe.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .amount(request.amount())
                .currency(request.currency().toLowerCase())
                .status(OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }
}
