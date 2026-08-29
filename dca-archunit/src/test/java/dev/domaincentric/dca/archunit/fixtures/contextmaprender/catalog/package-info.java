@BoundedContext(name = "Product Catalog", description = "Master data of sellable products")
@Partnership(context = "cart", rationale = "Catalog and cart evolve together")
package dev.domaincentric.dca.archunit.fixtures.contextmaprender.catalog;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
