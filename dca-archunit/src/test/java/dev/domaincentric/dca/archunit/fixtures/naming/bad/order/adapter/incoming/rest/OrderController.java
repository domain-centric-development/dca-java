package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.adapter.incoming.rest;

import org.springframework.web.bind.annotation.RestController;

// DCA-NAM-006: @RestController not ending with 'Resource'
@RestController
public final class OrderController {}
