package com.Stripe.service;

import com.Stripe.dto.CreateOrderRequest;
import com.Stripe.entity.Order;
import com.Stripe.entity.OrderStatus;
import com.Stripe.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
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

        orderRepository.save(order);

        log.info("Order {} created for {} {}",
                order.getId(), order.getAmount(), order.getCurrency());

        return order;
    }
}
