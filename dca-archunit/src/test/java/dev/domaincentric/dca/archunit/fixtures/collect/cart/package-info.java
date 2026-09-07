/** Two dangling upstreams (DCA-MAP-004) and two undeclared dependencies (DCA-MAP-011). */
@BoundedContext(name = "cart")
@Upstream(
    context = "missingOne",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API)
@Upstream(
    context = "missingTwo",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API)
package dev.domaincentric.dca.archunit.fixtures.collect.cart;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
