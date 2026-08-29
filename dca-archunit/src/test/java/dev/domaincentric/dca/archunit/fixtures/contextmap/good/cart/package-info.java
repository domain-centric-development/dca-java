@BoundedContext(name = "cart", description = "Shopping cart")
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = Upstream.Consumes.API,
    rationale = "Product data is translated into cart line items")
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.EVENTS,
    rationale = "Price change events are consumed as published")
@Upstream(
    context = "pricing",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API,
    status = Upstream.Status.PLANNED,
    rationale = "Dynamic pricing not yet integrated")
@ExternalUpstream(
    name = "Payment Gateway",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.OUTBOUND,
    contractPackages = "dev.domaincentric.dca.archunit.fixtures.contextmap.good.external.payment..")
@ExternalUpstream(
    name = "Tax Service",
    translation = Upstream.Translation.CONFORMIST,
    interaction = ExternalUpstream.Interaction.OUTBOUND)
@ApplicationModule(
    allowedDependencies = {"catalog :: api", "catalog::events", "pricing :: api", "sharedkernel"})
package dev.domaincentric.dca.archunit.fixtures.contextmap.good.cart;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.springframework.modulith.ApplicationModule;
