package com.demo.tests.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Booking {

    @JsonProperty("roomid") public int roomid;
    @JsonProperty("firstname") public String firstname;
    @JsonProperty("lastname") public String lastname;
    @JsonProperty("depositpaid") public boolean depositpaid;
    @JsonProperty("bookingdates") public BookingDates bookingdates;

    public Booking(int roomid, String fn, String ln, boolean deposit,
                   BookingDates dates) {
        this.roomid = roomid;
        this.firstname = fn;
        this.lastname = ln;
        this.depositpaid = deposit;
        this.bookingdates = dates;
    }
}
