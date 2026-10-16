package com.taxibooking.model; //Passenger

/** A customer account. Passenger ID looks like P001, P002 ... */
public class Passenger extends User {
    public static final String ROLE = "PASSENGER";

    public Passenger() {
        setRole(ROLE);
    }

    public String getPassengerId() { return getUserId(); }
}
