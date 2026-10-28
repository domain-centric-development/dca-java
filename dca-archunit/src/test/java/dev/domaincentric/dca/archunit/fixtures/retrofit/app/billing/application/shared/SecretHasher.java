package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.shared;

import dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary.LocalPort;
import dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary.OutputPort;

/** Never leaves the process. */
public interface SecretHasher extends OutputPort, LocalPort {
  String hash(String secret);
}
