package dev.domaincentric.dca.archunit.modulith;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaLayout;
import java.util.regex.Pattern;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Builds Spring Modulith's {@link ApplicationModules} for a DCA layout, with the one filter every
 * project otherwise rediscovers the hard way.
 *
 * <p>Test classes that live directly in the base package — the architecture tests themselves —
 * would otherwise become a synthetic <em>root module</em>, and Modulith then reports that root
 * module depending on non-exposed types (a shared-kernel value object, a context's internals). The
 * filter excludes them by their <b>full</b> name, so inner classes and Groovy closure classes
 * ({@code FooTest$1}, {@code FooSpec$_check_closure1}) are excluded with their owner; a {@code
 * simpleName} check misses those.
 */
public final class ModulithModules {

  /**
   * {@code FooTest}, {@code FooTests}, {@code FooSpec}, {@code FooIT} — and every nested class of
   * them.
   */
  private static final Pattern TEST_CLASS = Pattern.compile(".*(Test|Tests|Spec|IT)(\\$.*)?$");

  private ModulithModules() {}

  /** The application modules below the layout's base package, test classes excluded. */
  public static ApplicationModules of(DcaLayout layout) {
    if (layout == null) {
      throw new IllegalArgumentException("layout must not be null");
    }
    return ApplicationModules.of(layout.basePackage(), testClasses());
  }

  /**
   * The predicate handed to Modulith as {@code ignored}; exposed for projects composing their own.
   */
  public static DescribedPredicate<JavaClass> testClasses() {
    return new DescribedPredicate<>("test classes (by name, nested classes included)") {
      @Override
      public boolean test(JavaClass javaClass) {
        return isTestClass(javaClass.getName());
      }
    };
  }

  /** Whether a fully qualified class name denotes a test class or a class nested in one. */
  public static boolean isTestClass(String fullyQualifiedName) {
    if (fullyQualifiedName == null) {
      return false;
    }
    String simple = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    return TEST_CLASS.matcher(simple).matches();
  }
}
