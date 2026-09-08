package dev.domaincentric.dca.buildingblocks.hexagonal.port.out;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.Optional;

/**
 * Base interface for Repositories.
 *
 * <p>Repositories provide a collection-like interface for accessing Aggregate Roots. They
 * encapsulate the logic for retrieving and persisting aggregates, presenting the illusion of an
 * in-memory collection.
 *
 * <p><b>Key Principles:</b>
 *
 * <ul>
 *   <li>One Repository per Aggregate Root (not per Entity)
 *   <li>Repository implementations belong in secondary adapters (infrastructure layer)
 *   <li>Use domain language in method names (not generic CRUD)
 *   <li>Return domain objects, never infrastructure objects
 * </ul>
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Interface resides in the application layer as an output port (e.g., {@code
 *       product.application.shared.ProductRepository})
 *   <li>Implementation resides in an outgoing adapter (e.g., {@code
 *       product.adapter.outgoing.persistence.InMemoryProductRepository})
 *   <li>Methods use ubiquitous language (e.g., {@code findBySku()}, {@code findByCategory()})
 *   <li>Should NOT have Spring annotations in the interface
 *   <li>Collections should be immutable when returned
 * </ul>
 *
 * <p><b>Common Methods:</b>
 *
 * <ul>
 *   <li>{@code findById(ID)} - Retrieve aggregate by its unique identifier
 *   <li>{@code save(T)} - Add or update an aggregate (collection metaphor)
 *   <li>{@code deleteById(ID)} - Remove an aggregate from the collection
 * </ul>
 *
 * <p>These three are the deliberate minimum: what every aggregate's life cycle needs and nothing a
 * concrete port would have to override. The port extends them freely with the questions its use
 * cases ask - {@code findBySku(SKU)}, {@code existsBySku(SKU)}, {@code count()}, {@code
 * deleteAll()}; the marker adds no generic {@code existsById} or {@code findAll}, because whether
 * such a question is asked at all is a decision of the owning context, not of the building block.
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * // Output port in the application layer
 * public interface ProductRepository extends Repository&lt;Product, ProductId&gt; {
 *   Optional&lt;Product&gt; findBySku(SKU sku);
 *   List&lt;Product&gt; findByCategory(Category category);
 *   boolean existsBySku(SKU sku);
 * }
 *
 * // Secondary adapter implementation
 * {@literal @}Repository
 * public class InMemoryProductRepository implements ProductRepository {
 *   // Implementation using in-memory storage
 * }
 * </pre>
 *
 * <p><b>Pattern:</b> Repositories mediate between the domain and data mapping layers using a
 * collection-like interface for accessing domain objects.
 *
 * <p><b>References:</b>
 *
 * <ul>
 *   <li>Eric Evans' Domain-Driven Design (2003), Chapter 6: "The Life Cycle of a Domain Object"
 *   <li>Vaughn Vernon's Implementing Domain-Driven Design (2013), Chapter 12: "Repositories"
 *   <li>Martin Fowler's <a href="https://martinfowler.com/eaaCatalog/repository.html">Repository
 *       Pattern</a>
 * </ul>
 *
 * @param <T> the aggregate root type
 * @param <ID> the aggregate root ID type
 * @see <a href="https://www.domainlanguage.com/ddd/">Domain-Driven Design Reference</a>
 */
public interface Repository<T extends AggregateRoot<T, ID>, ID extends Id> extends OutputPort {

  /**
   * Finds an aggregate by its unique identifier.
   *
   * @param id the aggregate ID
   * @return an Optional containing the aggregate if found, empty otherwise
   */
  Optional<T> findById(ID id);

  /**
   * Saves an aggregate to the repository.
   *
   * <p>This method adds a new aggregate or updates an existing one. The repository handles the
   * distinction based on the aggregate's identity.
   *
   * <p>After saving, domain events should be published by the application service.
   *
   * @param aggregate the aggregate to save
   * @return the saved aggregate
   */
  T save(T aggregate);

  /**
   * Deletes an aggregate from the repository by its ID.
   *
   * <p>This method removes the aggregate from the collection. If the aggregate doesn't exist, the
   * behavior is implementation-specific (may throw exception or silently succeed).
   *
   * @param id the ID of the aggregate to delete
   */
  void deleteById(ID id);
}
