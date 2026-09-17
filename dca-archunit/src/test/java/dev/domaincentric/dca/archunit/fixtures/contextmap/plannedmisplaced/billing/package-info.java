@BoundedContext(name = "billing")
@Upstream(
    context = "ledger",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = Upstream.Consumes.API,
    status = Upstream.Status.PLANNED)
@Upstream(
    context = "ledger",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.EVENTS,
    status = Upstream.Status.PLANNED)
@ExternalUpstream(
    name = "Tax Authority",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.OUTBOUND,
    status = Upstream.Status.PLANNED,
    contractPackages =
        "dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.external.tax..")
package dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
