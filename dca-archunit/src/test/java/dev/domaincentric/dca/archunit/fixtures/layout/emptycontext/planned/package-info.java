/**
 * A bounded context that is declared before it has any code — the boundary of meaning exists, the
 * model does not yet. Legitimate: a context is a boundary of language and ownership, not a file
 * count.
 */
@BoundedContext(name = "Planned", description = "Declared now, filled later")
package dev.domaincentric.dca.archunit.fixtures.layout.emptycontext.planned;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
