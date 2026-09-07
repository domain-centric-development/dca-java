package dev.domaincentric.dca.buildingblocks.hexagonal.port.in;

/**
 * Marker interface for Input Ports (Hexagonal Architecture) / Use Cases (Clean Architecture).
 *
 * <p>An input port represents an entry point to the application layer, defining a single use case
 * or application feature. In Hexagonal Architecture, input ports are called by primary/driving
 * adapters (e.g., REST controllers, event consumers, CLI handlers).
 *
 * <p><b>Characteristics of Input Ports:</b>
 *
 * <ul>
 *   <li>Represent a single user action or system operation
 *   <li>Define the interface that use cases implement
 *   <li>Accept Input models (Commands or Queries) as parameters
 *   <li>Return Output models, not domain entities
 *   <li>Technology-agnostic (no framework dependencies)
 * </ul>
 *
 * <p><b>Input/Output Pattern:</b> Input ports accept Input models and return Output models to
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
 * @param <OUTPUT> the output model type (Response)
 */
public interface UseCase<INPUT, OUTPUT> extends InputPort {

  /**
   * Executes this use case with the given input.
   *
   * @param input the use case input (Command or Query)
   * @return the use case output (Response)
   */
  OUTPUT execute(INPUT input);
}
