/**
 * A module that is deliberately not a bounded context, grouped below an intermediate package. It
 * declares itself with the framework's module annotation instead of {@code @BoundedContext}.
 */
@ApplicationModule(allowedDependencies = {"sharedkernel"})
package dev.domaincentric.dca.archunit.fixtures.layout.groupedmodule.generic.backoffice;

import org.springframework.modulith.ApplicationModule;
