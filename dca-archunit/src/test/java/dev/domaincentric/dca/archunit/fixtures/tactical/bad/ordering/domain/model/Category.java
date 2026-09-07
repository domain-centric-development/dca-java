package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DCA-TAC-003: containers of the aggregate's own type hold other aggregate instances - a list, an
 * optional, an array, a map and a nested list are all reported. The direct field {@code root} of
 * the own type is the one tolerated shape (a self-reference is not another aggregate).
 */
public final class Category extends BaseAggregateRoot<Category, CategoryId> {
  private final CategoryId id;
  private final Category root;
  private final List<Category> children = List.of();
  private final Optional<Category> parent = Optional.empty();
  private final Category[] siblings = new Category[0];
  private final Map<String, Category> byName = Map.of();
  private final List<List<Category>> tree = List.of();

  public Category(CategoryId id, Category root) {
    this.id = id;
    this.root = root;
  }

  @Override
  public CategoryId id() {
    return id;
  }

  public Category root() {
    return root;
  }

  public List<Category> children() {
    return children;
  }

  public Optional<Category> parent() {
    return parent;
  }

  public Category[] siblings() {
    return siblings;
  }

  public Map<String, Category> byName() {
    return byName;
  }

  public List<List<Category>> tree() {
    return tree;
  }
}
