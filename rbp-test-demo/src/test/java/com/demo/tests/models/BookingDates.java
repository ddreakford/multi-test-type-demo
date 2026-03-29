package com.demo.tests.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookingDates {

    @JsonProperty("checkin") public String checkin;
    @JsonProperty("checkout") public String checkout;

    public BookingDates(String in, String out) {
        this.checkin = in;
        this.checkout = out;
    }
}
