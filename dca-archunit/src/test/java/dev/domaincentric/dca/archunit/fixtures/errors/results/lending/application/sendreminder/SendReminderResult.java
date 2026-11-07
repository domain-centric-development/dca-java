package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.sendreminder;

/**
 * A closed result whose outcomes are both successes: sent now, or sent before. Neither refuses the
 * request, so neither carries the refusal marker.
 */
public sealed interface SendReminderResult
    permits SendReminderResult.Sent, SendReminderResult.AlreadySent {

  record Sent(String loanId) implements SendReminderResult {}

  record AlreadySent(String loanId) implements SendReminderResult {}
}
