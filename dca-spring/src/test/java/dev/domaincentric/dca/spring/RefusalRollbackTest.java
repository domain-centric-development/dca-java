package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.buildingblocks.application.Refusal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** A {@code @Transactional} method that returns a refusal rolls back, wired by auto-config. */
class RefusalRollbackTest {

  sealed interface ReserveResult permits Reserved, SeatTaken {}

  record Reserved(String seat) implements ReserveResult {}

  record SeatTaken(String seat) implements ReserveResult, Refusal {}

  /** Holds what was committed: a write becomes visible only when its transaction commits. */
  static final class CommittedWrites {
    final List<String> rows = new ArrayList<>();

    void write(String row) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              rows.add(row);
            }
          });
    }
  }

  static class InnerStep {
    private final CommittedWrites writes;

    InnerStep(CommittedWrites writes) {
      this.writes = writes;
    }

    @Transactional
    public void writeThenFail(String row) {
      writes.write(row);
      throw new IllegalStateException("refused by the model");
    }

    @Transactional
    public ReserveResult writeThenRefuse(String row) {
      writes.write(row);
      return new SeatTaken(row);
    }
  }

  static class ReserveSeat {
    private final CommittedWrites writes;
    private final InnerStep inner;

    ReserveSeat(CommittedWrites writes, InnerStep inner) {
      this.writes = writes;
      this.inner = inner;
    }

    @Transactional
    public ReserveResult writeThenRefuse(String seat) {
      writes.write(seat);
      return new SeatTaken(seat);
    }

    @Transactional
    public ReserveResult writeThenSucceed(String seat) {
      writes.write(seat);
      return new Reserved(seat);
    }

    @Transactional
    public ReserveResult catchInnerFailureAndRefuse(String seat) {
      writes.write(seat);
      try {
        inner.writeThenFail(seat + "-inner");
      } catch (IllegalStateException refused) {
        return new SeatTaken(seat);
      }
      return new Reserved(seat);
    }

    @Transactional
    public ReserveResult innerRefusesOuterSucceeds(String seat) {
      inner.writeThenRefuse(seat);
      return new Reserved(seat);
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class Application {
    @Bean
    RecordingTransactionManager transactionManager() {
      return new RecordingTransactionManager();
    }

    @Bean
    CommittedWrites committedWrites() {
      return new CommittedWrites();
    }

    @Bean
    InnerStep innerStep(CommittedWrites writes) {
      return new InnerStep(writes);
    }

    @Bean
    ReserveSeat reserveSeat(CommittedWrites writes, InnerStep inner) {
      return new ReserveSeat(writes, inner);
    }
  }

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DcaSpringAutoConfiguration.class))
          .withUserConfiguration(Application.class);

  @Test
  void aWriteFollowedByAReturnedRefusalIsRolledBack() {
    runner.run(
        ctx -> {
          ReserveResult result = ctx.getBean(ReserveSeat.class).writeThenRefuse("A1");

          assertInstanceOf(SeatTaken.class, result, "the refusal is returned, not thrown");
          assertTrue(ctx.getBean(CommittedWrites.class).rows.isEmpty(), "nothing committed");
          assertEquals(List.of("begin", "rollback"), log(ctx));
        });
  }

  @Test
  void aReturnedSuccessCommits() {
    runner.run(
        ctx -> {
          ctx.getBean(ReserveSeat.class).writeThenSucceed("A1");

          assertEquals(List.of("A1"), ctx.getBean(CommittedWrites.class).rows);
          assertEquals(List.of("begin", "commit"), log(ctx));
        });
  }

  @Test
  void aRefusalAfterACaughtInnerFailureRollsBackWithoutUnexpectedRollback() {
    runner.run(
        ctx -> {
          ReserveResult result = ctx.getBean(ReserveSeat.class).catchInnerFailureAndRefuse("A1");

          assertInstanceOf(SeatTaken.class, result);
          assertTrue(ctx.getBean(CommittedWrites.class).rows.isEmpty());
          assertEquals(List.of("begin", "rollback"), log(ctx));
        });
  }

  @Test
  void aSuccessAfterAnInnerRefusalFailsLikeAfterAnInnerFailure() {
    runner.run(
        ctx -> {
          assertThrows(
              UnexpectedRollbackException.class,
              () -> ctx.getBean(ReserveSeat.class).innerRefusesOuterSucceeds("A1"));
          assertTrue(ctx.getBean(CommittedWrites.class).rows.isEmpty());
        });
  }

  @Test
  void anExceptionStillRollsBack() {
    runner.run(
        ctx -> {
          assertThrows(
              IllegalStateException.class, () -> ctx.getBean(InnerStep.class).writeThenFail("A1"));
          assertTrue(ctx.getBean(CommittedWrites.class).rows.isEmpty());
          assertEquals(List.of("begin", "rollback"), log(ctx));
        });
  }

  @Test
  void switchedOffARefusalCommits() {
    runner
        .withPropertyValues("dca.spring.refusal-rollback.enabled=false")
        .run(
            ctx -> {
              ctx.getBean(ReserveSeat.class).writeThenRefuse("A1");

              assertEquals(List.of("A1"), ctx.getBean(CommittedWrites.class).rows);
              assertEquals(List.of("begin", "commit"), log(ctx));
            });
  }

  private static List<String> log(ApplicationContext ctx) {
    return ctx.getBean(RecordingTransactionManager.class).log;
  }
}
