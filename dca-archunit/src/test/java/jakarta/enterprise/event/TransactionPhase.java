package jakarta.enterprise.event;

/** Test shim — mirrors the CDI transaction phases. */
public enum TransactionPhase {
  IN_PROGRESS,
  BEFORE_COMPLETION,
  AFTER_COMPLETION,
  AFTER_FAILURE,
  AFTER_SUCCESS
}
