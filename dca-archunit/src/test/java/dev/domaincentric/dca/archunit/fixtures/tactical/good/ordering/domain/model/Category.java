package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.util.List;

/**
 * A self-reference of the own type is tolerated by DCA-TAC-003; other instances of the same
 * aggregate are referenced by their ids.
 */
public final class Category extends BaseAggregateRoot<Category, CategoryId> {
  private final CategoryId id;
  private final Category parent;
  private final List<CategoryId> childIds = List.of();

  public Category(CategoryId id, Category parent) {
    this.id = id;
    this.parent = parent;
  }

  @Override
  public CategoryId id() {
    return id;
  }

  public Category parent() {
    return parent;
  }

  public List<CategoryId> childIds() {
    return childIds;
  }
}
