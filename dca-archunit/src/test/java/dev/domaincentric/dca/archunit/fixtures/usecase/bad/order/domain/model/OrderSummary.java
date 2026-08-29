package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.adapter.incoming.web.OrderDto;

// DCA-USE-010: domain depends on a DTO
public final class OrderSummary {
  public OrderDto dto;
}
