package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model.Ward;
import dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model.WardId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface WardRepository extends Repository<Ward, WardId> {}
