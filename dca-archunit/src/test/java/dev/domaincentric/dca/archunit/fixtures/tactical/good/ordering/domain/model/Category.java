package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.util.List;

/** Other instances of the same aggregate are referenced by their ids. */
public final class Category extends BaseAggregateRoot<Category, CategoryId> {
  private final CategoryId id;
  private final CategoryId parent;
  private final List<CategoryId> childIds = List.of();

  public Category(CategoryId id, CategoryId parent) {
    this.id = id;
    this.parent = parent;
  }

  @Override
  public CategoryId id() {
    return id;
  }

  public CategoryId parent() {
    return parent;
  }

  public List<CategoryId> childIds() {
    return childIds;
  }
}
