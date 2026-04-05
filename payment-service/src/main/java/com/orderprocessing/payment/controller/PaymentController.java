package com.orderprocessing.payment.controller;

import com.orderprocessing.payment.dto.PaymentResponse;
import com.orderprocessing.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderId}")
    public PaymentResponse getPayment(@PathVariable UUID orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }
}
