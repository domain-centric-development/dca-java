package dev.domaincentric.dca.archunit.fixtures.layout.groupedmodule.generic.backoffice.application.report;

/** A query in a grouped non-context module — the layer and use-case rules must still see it. */
public record BuildReportQuery(String from) {}
