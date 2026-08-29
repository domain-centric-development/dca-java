package dev.domaincentric.dca.archunit;

import java.util.List;

/**
 * A named group of {@link DcaRule}s, e.g. all tactical DDD rules. Rule sets are stateless with
 * respect to the code under test — they receive the {@link DcaArchitecture} at check time.
 */
public interface DcaRuleSet {

  /** Short name, e.g. {@code "tactical"}. Also the identifier prefix segment in lower case. */
  String name();

  /** The rules of this set, in catalog order. */
  List<DcaRule> rules();
}
