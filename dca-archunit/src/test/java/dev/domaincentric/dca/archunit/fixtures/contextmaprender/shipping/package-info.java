@BoundedContext(name = "Shipping", description = "Parcel dispatch")
// Declared out of order on purpose: the renderer sorts, so "cart" must come out before "catalog"
// however these two are written down. Same fixture shape in the .NET twin.
@Upstream(
    context = "catalog",
    translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
    via = Upstream.Consumes.API,
    rationale = "Dimensions and weight of the articles to dispatch")
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
