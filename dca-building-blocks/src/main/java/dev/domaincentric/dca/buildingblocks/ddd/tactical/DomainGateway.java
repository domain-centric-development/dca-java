package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for Domain Gateways.
 *
 * <p>A Domain Gateway is an interface declared in the <b>domain layer</b> through which the domain
 * obtains facts it does not hold, or delegates a technology-bound computation, without coupling the
 * domain to framework or infrastructure types. The implementation lives outside the domain
 * (typically in an outgoing adapter), but the contract is owned by the domain and expressed in
 * domain language.
 *
 * <p><b>Who calls it.</b> A domain service, the application (a use case), or a model method that
 * receives the gateway as a parameter. The aggregate, entity, value object, factory or
 * specification that needs it takes it in the signature of the method that needs it, and the domain
 * service or the use case passes it in. It never holds a gateway in a field, never looks one up
 * from a registry and never builds an implementation itself: the dependency stays visible in the
 * signature, and the model keeps no reference to the outside between two calls.
 *
 * <p><b>It never writes to the outside.</b> A gateway enriches the model with information so that
 * the model can decide; it does not change the state of any external system. Persisting, sending,
 * publishing and calling a remote operation that has an effect are the application's business,
 * through an output port. A computation that touches nothing outside the process — hashing a
 * password, converting a currency with a supplied rate — is a gateway, because no external state
 * changes.
 *
 * <p><b>How it differs from related concepts:</b>
 *
 * <ul>
 *   <li><b>vs Domain Service</b> — A {@link DomainService} contains pure domain logic; both
 *       interface and implementation live in the domain layer (framework-free). A Domain Gateway is
 *       a <i>port</i> to the outside world; only the interface is in the domain.
 *   <li><b>vs Output Port</b> — An Output Port (see {@code OutputPort}) lives in the application
 *       layer and is used by use cases. A Domain Gateway lives in the domain layer and is used by
 *       domain services, by use cases and by the model method it is passed to, to obtain the facts
 *       an invariant needs.
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
 *   <li>Called by a domain service, by the application, or by a model method that receives it as a
 *       parameter; never held in a field or looked up by the model
 *   <li>Read-only towards the outside: no external state is created, changed or removed
 * </ul>
 *
 * <p><b>Example use cases:</b>
 *
 * <ul>
 *   <li>Password hashing: the use case passes a {@code PasswordHasher} to {@code
 *       Account.register(..., hasher)}, which hashes the password through it
 *   <li>Availability check: a domain service asks the gateway and passes the answer to {@code
 *       Order.confirm(...)}
 *   <li>Tax rate lookup: the use case looks up the rate and hands it to the aggregate that computes
 *       the totals
 * </ul>
 *
 * <p><b>Reference:</b> The pattern starts from Vaughn Vernon's IDDD (2013) sample code ({@code
 * iddd_identityaccess}, an {@code EncryptionService} reached via the {@code DomainRegistry}),
 * generalized as a typed marker rather than a service locator. Where the sample's {@code User}
 * aggregate asks the {@code DomainRegistry} for the service, DCA passes the gateway in as a
 * parameter: no registry, no hidden dependency.
 */
public interface DomainGateway {}
