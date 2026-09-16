@BoundedContext(name = "billing")
@Upstream(
    context = "ledger",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API)
package dev.domaincentric.dca.archunit.fixtures.contextmap.nomodule.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
