@dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext(name = "consumer")
@dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream(
    context = "u",
    translation =
        dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream.Translation
            .ANTI_CORRUPTION_LAYER,
    via = dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream.Consumes.API)
@dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream(
    context = "v",
    translation =
        dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream.Translation
            .ANTI_CORRUPTION_LAYER,
    via = dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream.Consumes.API)
package dev.domaincentric.dca.archunit.fixtures.interaction.bad.consumer;
