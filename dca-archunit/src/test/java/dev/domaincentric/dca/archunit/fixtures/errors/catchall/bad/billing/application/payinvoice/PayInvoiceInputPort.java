package dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.payinvoice;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PayInvoiceInputPort extends UseCase<PayInvoiceCommand, PayInvoiceResult> {}
