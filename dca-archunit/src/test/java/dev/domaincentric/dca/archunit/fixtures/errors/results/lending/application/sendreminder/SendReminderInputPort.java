package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.sendreminder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface SendReminderInputPort extends UseCase<SendReminderCommand, SendReminderResult> {}
