@BoundedContext(name = "pricing")
@ExternalUpstream(
    name = "cart",
    translation = Upstream.Translation.CONFORMIST,
    interaction = ExternalUpstream.Interaction.INBOUND)
@Upstream(
    context = "warehouse",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.API,
    status = Upstream.Status.PLANNED)
@ApplicationModule
package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.pricing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.springframework.modulith.ApplicationModule;
