package dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.domain.model;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.domain.model.AlphaThing;

public record BetaThing(String id, AlphaThing back) {}
