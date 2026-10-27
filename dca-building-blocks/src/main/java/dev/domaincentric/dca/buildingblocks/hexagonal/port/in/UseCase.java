package dev.domaincentric.dca.buildingblocks.hexagonal.port.in;

/**
 * The most common kind of {@link InputPort}: one operation that takes a command or query and
 * answers with a result - a Use Case in Clean Architecture terms.
 *
 * <p>Every use case is an input port; not every input port is a use case. An input port that only
 * receives an incoming event or serves operations without a result model extends {@link InputPort}
 * directly. A use case is called by primary/driving adapters (e.g., REST controllers, event
 * consumers, CLI handlers).
 *
 * <p><b>Characteristics of a Use Case:</b>
 *
 * <ul>
 *   <li>Represents a single user action or system operation
 *   <li>Is the interface the application-layer implementation implements
 *   <li>Accepts an Input model (Command or Query) as its parameter
 *   <li>Returns an Output model, not a domain entity
 *   <li>Technology-agnostic (no framework dependencies)
 * </ul>
 *
 * <p><b>Input/Output Pattern:</b> A use case accepts an Input model and returns an Output model to
 * decouple the application layer from presentation and infrastructure concerns.
 *
 * <p><b>Example:</b>
 *
 * <pre>{@code
 * public interface CreateProductInputPort extends UseCase<CreateProductCommand, CreateProductResult> {}
 *
 * public class CreateProductUseCase implements CreateProductInputPort {
 *     @Override
 *     public CreateProductResult execute(CreateProductCommand input) {
 *         // Use case implementation
 *     }
 * }
 * }</pre>
 *
 * <p><b>Naming Convention:</b> the input port interface is {@code {Action}{Entity}InputPort}
 * ({@code CreateProductInputPort}, {@code UpdateProductPriceInputPort}); the class implementing it
 * is {@code {Action}{Entity}UseCase}. The interface inherits {@code execute} from {@code UseCase}
 * and needs no method of its own.
 *
 * <p><b>References:</b>
 *
 * <ul>
 *   <li>Alistair Cockburn - Hexagonal Architecture (Ports &amp; Adapters)
 *   <li>Robert C. Martin - Clean Architecture (Chapter 19-20: Use Cases)
 *   <li>Tom Hombergs - Get Your Hands Dirty on Clean Architecture
 * </ul>
 *
 * @param <INPUT> the input model type (Command or Query)
 * @param <OUTPUT> the output model type (Result)
 */
public interface UseCase<INPUT, OUTPUT> extends InputPort {

  /**
   * Executes this use case with the given input.
   *
   * @param input the use case input (Command or Query)
   * @return the use case output (Result)
   */
  OUTPUT execute(INPUT input);
}
