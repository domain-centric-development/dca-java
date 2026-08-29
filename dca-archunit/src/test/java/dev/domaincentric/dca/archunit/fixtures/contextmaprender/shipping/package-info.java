@BoundedContext(name = "Shipping", description = "Parcel dispatch")
@Upstream(
    context = "cart",
    translation = Upstream.Translation.CONFORMIST,
    via = Upstream.Consumes.EVENTS,
    status = Upstream.Status.PLANNED,
    rationale = "Ship what was ordered")
@ExternalUpstream(
    name = "Carrier API",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    interaction = ExternalUpstream.Interaction.OUTBOUND,
    protocol = "REST",
    exchanges = "Shipment labels",
    rationale = "Labels are printed by the carrier")
package dev.domaincentric.dca.archunit.fixtures.contextmaprender.shipping;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
