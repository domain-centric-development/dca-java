package com.acme.shop.cart.application.getcart;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface GetCartInputPort extends UseCase<GetCartQuery, GetCartResult> {}
