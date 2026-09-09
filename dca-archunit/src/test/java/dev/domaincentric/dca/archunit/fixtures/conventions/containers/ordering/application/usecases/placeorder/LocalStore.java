package dev.domaincentric.dca.archunit.fixtures.conventions.containers.ordering.application.usecases.placeorder;

public interface LocalStore extends dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store {
  Object findById(String id);
}
