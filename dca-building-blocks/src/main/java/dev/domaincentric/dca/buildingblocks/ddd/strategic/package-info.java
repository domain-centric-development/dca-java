/**
 * DDD Strategic Patterns - Context Boundaries.
 *
 * <p>Annotations for Domain-Driven Design strategic patterns that define boundaries and
 * relationships between domain models:
 *
 * <ul>
 *   <li>{@link BoundedContext} - Explicit boundary for a domain model
 *   <li>{@link dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.SharedKernel SharedKernel} - Curated subset shared between contexts
 *   <li>{@link dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService OpenHostService} - Public API exposed to other contexts
 * </ul>
 *
 * <p><b>Usage:</b> These annotations are typically applied to {@code package-info.java} files to
 * document architectural boundaries.
 *
 * <p><b>References:</b>
 *
 * <ul>
 *   <li>Eric Evans' Domain-Driven Design (2003), Strategic Design chapters
 *   <li>Vaughn Vernon's Implementing Domain-Driven Design (2013), Context Mapping
 * </ul>
 */
package dev.domaincentric.dca.buildingblocks.ddd.strategic;
