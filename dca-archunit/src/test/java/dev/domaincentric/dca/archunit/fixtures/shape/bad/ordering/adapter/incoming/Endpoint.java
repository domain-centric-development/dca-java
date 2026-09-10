package dev.domaincentric.dca.archunit.fixtures.shape.bad.ordering.adapter.incoming;

@org.springframework.stereotype.Controller
public record Endpoint(
    dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository repository) {}
