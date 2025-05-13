package com.ecommerce.project.service;

import com.ecommerce.project.payload.StripePaymentDto;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Service;

@Service
public interface StripeService {
    PaymentIntent paymentIntent(StripePaymentDto stripePaymentDto) throws StripeException;
}
