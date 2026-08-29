@BoundedContext(name = "catalog", description = "Product catalog")
@Partnership(context = "pricing", rationale = "Catalog and pricing evolve together")
@ApplicationModule
package dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import org.springframework.modulith.ApplicationModule;
