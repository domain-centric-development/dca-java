package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public record OrderCancelled(UUID eventId, Instant occurredOn) implements DomainEvent {}
