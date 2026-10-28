package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.events;

import dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary.IntegrationEvent;
import dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary.IntegrationEventType;

@IntegrationEventType(name = "billing.invoice-paid", version = 2)
public record InvoicePaid(String invoiceId) implements IntegrationEvent {}
