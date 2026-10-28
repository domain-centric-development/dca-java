package jakarta.enterprise.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test shim — mirrors the CDI observer annotation and its phase attribute. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Observes {
  TransactionPhase during() default TransactionPhase.IN_PROGRESS;
}
