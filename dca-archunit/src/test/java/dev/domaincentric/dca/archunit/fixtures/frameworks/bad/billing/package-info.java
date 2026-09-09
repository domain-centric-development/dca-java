/** Violations only a non-Spring preset can see: CDI and JPA annotations on the domain model. */
@BoundedContext(name = "Billing", description = "Invoices")
package dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
