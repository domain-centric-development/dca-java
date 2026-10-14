@BoundedContext(name = "billing", description = "Fixture context")
@Upstream(
    context = "ledger",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = Upstream.Consumes.API,
    rationale = "Ledger balances are translated into invoice amounts")
@ApplicationModule(allowedDependencies = {"ledger :: api", "ledger :: events", "sharedkernel"})
package dev.domaincentric.dca.archunit.fixtures.contextmap.unused.bad.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.springframework.modulith.ApplicationModule;
