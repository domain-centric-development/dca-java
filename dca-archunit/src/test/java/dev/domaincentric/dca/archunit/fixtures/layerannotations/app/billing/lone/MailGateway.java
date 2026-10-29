package dev.domaincentric.dca.archunit.fixtures.layerannotations.app.billing.lone;

import dev.domaincentric.dca.archunit.fixtures.layerannotations.app.billing.application.shared.MailPort;
import dev.domaincentric.dca.archunit.fixtures.layerannotations.vocabulary.OutgoingLayer;

/** Outgoing by its own annotation, in a package that declares nothing. */
@OutgoingLayer
public final class MailGateway implements MailPort {}
