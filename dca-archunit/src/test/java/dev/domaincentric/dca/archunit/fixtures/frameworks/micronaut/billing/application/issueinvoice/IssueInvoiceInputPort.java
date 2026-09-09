package dev.domaincentric.dca.archunit.fixtures.frameworks.micronaut.billing.application.issueinvoice;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface IssueInvoiceInputPort extends UseCase<IssueInvoiceCommand, IssueInvoiceResult> {}
