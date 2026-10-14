package jakarta.servlet.http;

/** Test shim — mirrors the platform type by name only. */
public interface HttpSession {
  Object getAttribute(String name);

  void setAttribute(String name, Object value);
}
