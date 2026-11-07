package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for an Aggregate Root that belongs to someone.
 *
 * <p>An owned aggregate is reachable only through its owner. Its Repository therefore declares a
 * lookup that takes the aggregate's id <em>and</em> the owner's id, and a use case acting for a
 * caller loads it through that lookup — never by id alone. An object loaded by its id alone is
 * reachable by anyone who knows or guesses the id: the insecure direct object reference of broken
 * access control (OWASP Top 10, A01).
 *
 * <p>The type argument names the owner's id type: {@code MedicalRecord implements
 * Owned<PatientId>}, {@code Workspace implements Owned<TenantId>}. The marker carries no method;
 * the owner is a field of the aggregate like any other, and the owner id type is what the
 * Repository's scoped lookup takes.
 *
 * <p><b>What it says, and what not:</b>
 *
 * <ul>
 *   <li>"Has an owner", not "always the same owner". The owner may change, but only through an
 *       explicit operation of the aggregate — a draft a visitor started as a guest, taken over by
 *       the account they sign into — called after the use case proved the caller's right to the
 *       change. That proof is logic of the use case, which no marker can verify; its tests prove
 *       it.
 *   <li>An owner need not be an account. A guest given a pseudonymous identity on the first request
 *       is an owner: the aggregate belongs to that identity, provided it comes from a signed token
 *       and is not guessable, never from a value the client names.
 *   <li>Where no identity exists and the aggregate's id itself is the secret — a long random id in
 *       a link, "whoever holds it may" — there is no owner to scope a lookup by, and the aggregate
 *       is not {@code Owned}. Its safety is the id's unguessability.
 * </ul>
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public final class MedicalRecord extends BaseAggregateRoot&lt;MedicalRecord, MedicalRecordId&gt;
 *     implements Owned&lt;PatientId&gt; {
 *   private PatientId patient;
 *   ...
 * }
 *
 * public interface MedicalRecordRepository extends Repository&lt;MedicalRecord, MedicalRecordId&gt; {
 *   Optional&lt;MedicalRecord&gt; findByIdForPatient(MedicalRecordId id, PatientId patient);
 * }
 * </pre>
 *
 * <p>Forgetting {@code implements Owned<…>} silences every rule that selects on it; marking the
 * aggregates that belong to someone is part of modelling them.
 *
 * @param <OWNER> the id type of the owner
 */
public interface Owned<OWNER extends Id> {}
