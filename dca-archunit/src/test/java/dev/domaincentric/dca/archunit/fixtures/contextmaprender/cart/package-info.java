@BoundedContext(name = "Shopping Cart", description = "Carts of guests and customers")
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = {Upstream.Consumes.API, Upstream.Consumes.EVENTS},
    rationale = "Cart needs product master data")
@Partnership(context = "catalog", rationale = "Catalog and cart evolve together")
package dev.domaincentric.dca.archunit.fixtures.contextmaprender.cart;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
