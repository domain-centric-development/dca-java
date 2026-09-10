package dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain;

public record Line(LineId id)
    implements dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity<Line, LineId> {}
