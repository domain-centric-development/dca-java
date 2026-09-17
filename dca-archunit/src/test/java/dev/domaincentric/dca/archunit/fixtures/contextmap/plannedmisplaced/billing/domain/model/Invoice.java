package dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.external.tax.TaxClient;
import dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.ledger.api.LedgerApi;
import dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.ledger.events.EntryPosted;

/**
 * Misplaced although PLANNED: the api contract outside the outgoing adapter (DCA-MAP-008), the
 * events contract in the domain (DCA-MAP-009), the external contract outside the adapter
 * (DCA-MAP-010). Placement is checked whatever the status; only the translation-site demand of
 * DCA-MAP-008 waits for IMPLEMENTED.
 */
public record Invoice(LedgerApi ledger, EntryPosted posted, TaxClient tax) {}
