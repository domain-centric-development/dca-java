package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for Domain Gateways.
 *
 * <p>A Domain Gateway is an interface declared in the <b>domain layer</b> that the domain itself
 * uses to consult external facts or delegate technology-bound operations, without coupling the
 * domain to framework or infrastructure types. The implementation lives outside the domain
 * (typically in an outgoing adapter), but the contract is owned by the domain and expressed in
 * domain language.
 *
 * <p><b>How it differs from related concepts:</b>
 *
 * <ul>
 *   <li><b>vs Domain Service</b> — A {@link DomainService} contains pure domain logic; both
 *       interface and implementation live in the domain layer (framework-free). A Domain Gateway is
 *       a <i>port</i> to the outside world; only the interface is in the domain.
 *   <li><b>vs Output Port</b> — An Output Port (see {@code OutputPort}) lives in the application
 *       layer and is used by use cases. A Domain Gateway lives in the domain layer and is used by
 *       aggregates, entities, or domain services to enforce invariants or perform domain-bound
 *       operations.
 *   <li><b>vs Repository</b> — A Repository persists and reconstitutes aggregates. A Domain Gateway
 *       exposes external capability (cryptography, availability check, geocoding, …).
 * </ul>
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Interface in the domain layer ({@code {context}/domain/gateway/})
 *   <li>No framework dependencies in the interface
 *   <li>Implementation in the outgoing adapter layer
 *   <li>Typically called by aggregates, entities, or domain services
 *   <li>Side-effect-free or read-only operations are the typical case
 * </ul>
 *
 * <p><b>Example use cases:</b>
 *
 * <ul>
 *   <li>Password hashing/verification ({@code PasswordHasher})
 *   <li>Shipping availability checks during {@code Order.confirm()}
 *   <li>Tax rate lookup when the aggregate computes totals
 * </ul>
 *
 * <p><b>Reference:</b> The pattern follows Vaughn Vernon's IDDD (2013) sample code ({@code
 * iddd_identityaccess}, {@code User} aggregate using {@code EncryptionService} via the {@code
 * DomainRegistry}), generalized as a typed marker rather than a service locator.
 */
public interface DomainGateway {}
