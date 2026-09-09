package dev.domaincentric.dca.buildingblocks.ddd.tactical;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalEventRegistrationTest {
  @TempDir Path directory;

  @Test
  void unrelatedFactoryCannotRegisterAnAggregatesEvents() throws Exception {
    Path source = directory.resolve("External.java");
    Files.writeString(
        source,
        "package consumer; import dev.domaincentric.dca.buildingblocks.ddd.tactical.*; class External { void register(BaseAggregateRoot<?,?> root, DomainEvent event) { root.registerEvent(event); } }");
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
      String classpath =
          Path.of(
                  BaseAggregateRoot.class
                      .getProtectionDomain()
                      .getCodeSource()
                      .getLocation()
                      .toURI())
              .toString();
      boolean accepted =
          compiler
              .getTask(
                  null,
                  files,
                  diagnostics,
                  List.of("-classpath", classpath, "-d", directory.toString()),
                  null,
                  files.getJavaFileObjects(source.toFile()))
              .call();
      assertFalse(accepted);
      assertTrue(
          diagnostics.getDiagnostics().stream()
              .anyMatch(d -> d.getMessage(Locale.ROOT).contains("protected access")),
          diagnostics.getDiagnostics().toString());
    }
  }
}
