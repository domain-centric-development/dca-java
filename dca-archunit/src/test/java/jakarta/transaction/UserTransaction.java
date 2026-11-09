package jakarta.transaction;

/** Test shim — mirrors the framework type by name only. */
public interface UserTransaction {

  void begin();

  void commit();
}
