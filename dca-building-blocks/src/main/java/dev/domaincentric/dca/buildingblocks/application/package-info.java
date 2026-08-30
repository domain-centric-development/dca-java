/**
 * Application-layer building blocks that are neither ports nor domain markers: execution
 * abstractions a use case relies on, implemented by infrastructure.
 *
 * <p>Currently {@link dev.domaincentric.dca.buildingblocks.application.TransactionBoundary} — the
 * explicit transaction boundary for use cases that also call remote-capable ports.
 */
package dev.domaincentric.dca.buildingblocks.application;
