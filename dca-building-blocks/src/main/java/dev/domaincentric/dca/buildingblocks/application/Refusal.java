package dev.domaincentric.dca.buildingblocks.application;

/**
 * Marker for a case of a use-case result that refuses the request.
 *
 * <p>A use case may answer an expected refusal - the addressed thing does not exist, the caller may
 * not act on it, the model refused the input - in one of two styles, chosen per bounded context: it
 * throws a named failure, or it returns the refusal as a case of a closed result (a sealed type
 * whose cases its caller switches over). Both are equally valid; within one context the style is
 * uniform. This marker belongs to the second style: every case of a closed result that refuses the
 * request implements it, and a case that reports success never does.
 *
 * <p><b>A refusal rolls back like a thrown failure.</b> A use case that wrote something before it
 * found out it has to refuse must not commit that write. A thrown failure rolls the transaction
 * back on its way out; a returned value would not, so the transaction boundary looks at the value
 * the work returns: when it is a {@code Refusal}, the boundary rolls back and hands the refusal to
 * the caller unchanged - see {@link TransactionBoundary}. An implementation of the platform's
 * declarative transactions does the same for a transactional use-case method that returns one. The
 * rule for good code stays "decide, then write" - check every refusal before the first write; the
 * roll-back is the net that catches a use case that does not.
 *
 * <p><b>Shape.</b> A refusal case carries the ids and figures the refusal compared, never display
 * text; the incoming adapter turns it into its protocol's answer, exactly as it would translate the
 * thrown failure. The marker declares no method: it says what a case is, the case's own fields say
 * what happened.
 *
 * <pre>{@code
 * public sealed interface ReserveSeatResult {
 *   record Reserved(SeatId seat) implements ReserveSeatResult {}
 *   record SeatTaken(SeatId seat) implements ReserveSeatResult, Refusal {}
 *   record ShowNotFound(ShowId show) implements ReserveSeatResult, Refusal {}
 * }
 * }</pre>
 *
 * <p>A closed result without any refusal case is no closed result: it is a plain result, and the
 * use case answers with the success itself.
 */
public interface Refusal {}
