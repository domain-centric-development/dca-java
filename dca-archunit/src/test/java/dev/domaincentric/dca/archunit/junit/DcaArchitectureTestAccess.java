package dev.domaincentric.dca.archunit.junit;

import dev.domaincentric.dca.archunit.DcaLayout;
import java.util.Properties;

/** Opens the package-private property glue of {@link DcaArchitectureTest} to tests elsewhere. */
public final class DcaArchitectureTestAccess {
  private DcaArchitectureTestAccess() {}

  public static DcaLayout applyConfiguredFramework(DcaLayout layout, Properties properties) {
    return DcaArchitectureTest.applyConfiguredFramework(layout, properties);
  }
}
