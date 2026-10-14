package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A code, typed: what it holds is a value of the model, not text for a page. */
public record BranchCode(String value) implements Value {}
