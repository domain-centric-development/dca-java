package dev.domaincentric.dca.archunit.fixtures.tactical.bad.sharedkernel.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.math.BigDecimal;

/**
 * DCA-TAC-009 (not final), DCA-TAC-010 (non-final field), DCA-TAC-011 (setter), DCA-TAC-012 (no
 * equals/hashCode).
 */
public class Money implements Value {
  private BigDecimal amount;
  private final String currency;

  public Money(BigDecimal amount, String currency) {
    this.amount = amount;
    this.currency = currency;
  }

  public BigDecimal amount() {
    return amount;
  }

  public String currency() {
    return currency;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
