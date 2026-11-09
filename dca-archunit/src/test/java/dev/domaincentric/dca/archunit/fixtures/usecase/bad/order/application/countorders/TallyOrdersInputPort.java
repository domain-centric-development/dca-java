package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

// DCA-USE-022: binds a bare number through an intermediate contract.
public interface TallyOrdersInputPort extends Answering<Integer> {}
