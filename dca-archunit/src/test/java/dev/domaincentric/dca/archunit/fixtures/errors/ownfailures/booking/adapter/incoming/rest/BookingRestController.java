package dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.application.placebooking.PlaceBookingCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.application.placebooking.PlaceBookingInputPort;
import dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.application.placebooking.SlotTakenException;

public final class BookingRestController {
  private final PlaceBookingInputPort placeBooking;

  public BookingRestController(PlaceBookingInputPort placeBooking) {
    this.placeBooking = placeBooking;
  }

  public String place(String slot) {
    try {
      return placeBooking.execute(new PlaceBookingCommand(slot)).bookingId();
    } catch (SlotTakenException taken) {
      return "409";
    }
  }
}
