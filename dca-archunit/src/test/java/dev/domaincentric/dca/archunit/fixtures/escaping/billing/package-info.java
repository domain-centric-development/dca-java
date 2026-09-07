@BoundedContext(name = "Billing", description = "Invoices | dunning")
@ExternalUpstream(
    name = "Tax \"Pro\" Service",
    translation = Upstream.Translation.CONFORMIST,
    interaction = ExternalUpstream.Interaction.OUTBOUND,
    rationale = "Rates come from the provider | never computed here")
package dev.domaincentric.dca.archunit.fixtures.escaping.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
