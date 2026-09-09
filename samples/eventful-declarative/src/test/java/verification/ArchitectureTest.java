package verification;
import dev.domaincentric.dca.archunit.*;
import org.junit.jupiter.api.*;
class ArchitectureTest {
 @TestFactory java.util.stream.Stream<DynamicTest> catalog() {
  var layout=DcaLayout.forBasePackage("acme").withFrameworkAnnotations(FrameworkAnnotations.spring());
  var arch=DcaArchitecture.load(layout);
  var rules=DcaRules.selectFlat(layout,DcaRuleSelection.all().onlySets("usecase","onion","hexagonal","naming","advanced","cycles","tactical"));
  System.out.println("Selected rules: "+rules.size());
  return rules.stream().map(r->DynamicTest.dynamicTest(r.id(),()->r.check(arch)));
 }
}
