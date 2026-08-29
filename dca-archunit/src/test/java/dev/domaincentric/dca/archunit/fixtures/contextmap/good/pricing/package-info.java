@BoundedContext(name = "pricing", description = "Price rules")
@Partnership(context = "catalog", rationale = "Catalog and pricing evolve together")
@ApplicationModule
package dev.domaincentric.dca.archunit.fixtures.contextmap.good.pricing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import org.springframework.modulith.ApplicationModule;
