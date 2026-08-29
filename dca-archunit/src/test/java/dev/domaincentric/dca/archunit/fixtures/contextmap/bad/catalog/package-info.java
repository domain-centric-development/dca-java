@BoundedContext(name = "catalog")
@Partnership(context = "pricing")
@ExternalUpstream(
    name = "Payment-Service",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.OUTBOUND)
@ExternalUpstream(
    name = "Payment Service",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.INBOUND)
@ApplicationModule
package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.catalog;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import org.springframework.modulith.ApplicationModule;
