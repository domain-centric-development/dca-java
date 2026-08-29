package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.adapter.incoming.web;

import org.springframework.stereotype.Controller;

// DCA-NAM-005: @Controller not ending with 'Controller'
@Controller
public final class OrderPage {}
