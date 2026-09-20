/**
 * Application-layer building blocks that are neither ports nor domain markers: the execution
 * abstraction a use case relies on and the failures it reports.
 *
 * <ul>
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.TransactionBoundary} — the explicit
 *       transaction boundary for use cases that also call remote-capable ports
 *   <li>{@link dev.domaincentric.dca.buildingblocks.application.UseCaseException} — the base of the
 *       failures a use case reports to its callers
 * </ul>
 */
package dev.domaincentric.dca.buildingblocks.application;
