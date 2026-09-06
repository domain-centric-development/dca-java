package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderLineItem;

// DCA-USE-015: part record in the result's package carrying an entity
public record OrderLine(OrderLineItem item) {}
