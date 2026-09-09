/** Its allowed dependencies live in the second module system's declaration. */
@BoundedContext(name = "Billing", description = "Invoices")
@Upstream(
    context = "ledger",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API,
    rationale = "Invoices are posted to the ledger")
@Module(allowedDependencies = {"ledger :: api"})
package dev.domaincentric.dca.archunit.fixtures.frameworks.modules.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.example.modules.Module;
