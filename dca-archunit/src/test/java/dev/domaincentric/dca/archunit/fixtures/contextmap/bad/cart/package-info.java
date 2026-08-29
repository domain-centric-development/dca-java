@BoundedContext(name = "cart")
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = Upstream.Consumes.API)
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.CONFORMIST,
    via = {Upstream.Consumes.API, Upstream.Consumes.EVENTS})
@Upstream(
    context = "pricing",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API)
@ExternalUpstream(
    name = "Payment Gateway",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.OUTBOUND,
    contractPackages = "dev.domaincentric.dca.archunit.fixtures.contextmap.bad.external.payment..")
@ApplicationModule(allowedDependencies = {"catalog :: api"})
package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.cart;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.springframework.modulith.ApplicationModule;
