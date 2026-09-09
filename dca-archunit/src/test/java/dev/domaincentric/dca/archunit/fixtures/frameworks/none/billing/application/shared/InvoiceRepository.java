package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.shared;

import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model.Invoice;
import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model.InvoiceId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface InvoiceRepository extends Repository<Invoice, InvoiceId> {}
