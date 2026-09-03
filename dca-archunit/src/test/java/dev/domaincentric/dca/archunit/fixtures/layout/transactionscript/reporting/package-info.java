/**
 * A bounded context in transaction-script style: a boundary of language with no rich domain model.
 * Generic subdomain — the use case reads a Store and maps to a Result, there is no aggregate to
 * protect. See the pattern-selection-per-subdomain decision.
 */
@BoundedContext(
    name = "Reporting",
    description = "Read-only operational reporting, transaction-script style")
package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
