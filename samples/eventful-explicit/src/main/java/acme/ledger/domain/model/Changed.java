package acme.ledger.domain.model;
public record Changed(java.util.UUID eventId,java.time.Instant occurredOn) implements dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent {}
