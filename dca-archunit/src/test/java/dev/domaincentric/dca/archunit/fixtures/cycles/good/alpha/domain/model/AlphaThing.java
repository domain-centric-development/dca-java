package dev.domaincentric.dca.archunit.fixtures.cycles.good.alpha.domain.model;

import dev.domaincentric.dca.archunit.fixtures.cycles.good.beta.domain.model.BetaThing;

public record AlphaThing(String id, BetaThing related) {}
