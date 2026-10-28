package dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.application.placebooking;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PlaceBookingInputPort extends UseCase<PlaceBookingCommand, PlaceBookingResult> {}
