package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderLineItem;

// DCA-USE-015: a part record shared between use cases in application.shared hides an entity.
public record OrderPart(OrderLineItem item, int position) {}
