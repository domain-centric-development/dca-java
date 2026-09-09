package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImmutableShapeTest {
  @Test
  void shapeIsCheckedForClassesAndRecords() {
    String root = Fixtures.ROOT + ".shape.";
    for (String id :
        List.of("DCA-USE-004", "DCA-USE-005", "DCA-USE-007", "DCA-ADV-001", "DCA-STR-008")) {
      Fixtures.rule(root + "good", id).check(Fixtures.arch(root + "good"));
      assertTrue(Fixtures.failure(root + "bad", id).getMessage().contains("State"));
    }
    Fixtures.rule(root + "good", "DCA-HEX-003").check(Fixtures.arch(root + "good"));
    assertTrue(Fixtures.failure(root + "bad", "DCA-HEX-003").getMessage().contains("Endpoint"));
    assertTrue(
        Fixtures.failure(root + "bad", "DCA-USE-004").getMessage().contains("RecordCommand"));
  }
}
