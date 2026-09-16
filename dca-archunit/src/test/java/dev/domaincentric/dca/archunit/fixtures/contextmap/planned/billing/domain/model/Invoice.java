package dev.domaincentric.dca.archunit.fixtures.contextmap.planned.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.planned.external.tax.TaxClient;
import dev.domaincentric.dca.archunit.fixtures.contextmap.planned.ledger.api.LedgerApi;
import dev.domaincentric.dca.archunit.fixtures.contextmap.planned.ledger.events.EntryPosted;

/**
 * Would violate DCA-MAP-008 (api contract outside the outgoing adapter), DCA-MAP-009 (events
 * contract in the domain) and DCA-MAP-010 (external contract outside the adapter) - but every
 * declaration is PLANNED, so none of the three enforces it yet.
 */
public record Invoice(LedgerApi ledger, EntryPosted posted, TaxClient tax) {}
