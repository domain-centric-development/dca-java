package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.domain.model;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.domain.model.BetaThing;

public record AlphaThing(String id, BetaThing related) {}
