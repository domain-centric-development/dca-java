/**
 * Application-layer building blocks that are neither ports nor domain markers: the execution
 * abstraction a use case relies on and the failures it reports.
 *
 * <ul>
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.TransactionBoundary} — the explicit
 *       transaction boundary for use cases that also call remote-capable ports; it rolls back a
 *       returned refusal like a thrown failure
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.Refusal} — the marker on a case of
 *       a closed use-case result that refuses the request
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.PortException} — the base of every
 *       failure a port's contract declares, with its two branches {@link
 *       dev.domaincentric.dca.buildingblocks.application.InputPortException} (declared by an input
 *       port, caught by the incoming adapter) and {@link
 *       dev.domaincentric.dca.buildingblocks.application.OutputPortException} (declared by an
 *       output port, raised by its adapter, caught by the use case)
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.UseCaseException} — the input-port
 *       failure a use case reports to its callers; raised only by a use case
 * </ul>
 */
package dev.domaincentric.dca.buildingblocks.application;
