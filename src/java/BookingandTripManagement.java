import java.util.ArrayList;
import java.util.Scanner;

public class BookingandTripManagement {


static class Booking {
    int bookingId;
    String passengerName;
    String pickup;
    String destination;
    String bookingTime;
    String driverName;
    String status;
    String cancellationReason;

    Booking(int bookingId, String passengerName, String pickup,
            String destination, String bookingTime) {

        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.pickup = pickup;
        this.destination = destination;
        this.bookingTime = bookingTime;
        this.driverName = "Not Assigned";
        this.status = "Pending";
        this.cancellationReason = "None";
    }

    void displayDetails() {
        System.out.println("\n--- Booking Details ---");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Passenger: " + passengerName);
        System.out.println("Pickup: " + pickup);
        System.out.println("Destination: " + destination);
        System.out.println("Booking Time: " + bookingTime);
        System.out.println("Driver: " + driverName);
        System.out.println("Status: " + status);
        System.out.println("Cancellation Reason: " + cancellationReason);
    }
}


    static Scanner input = new Scanner(System.in);
    static ArrayList<Booking> bookings = new ArrayList<>();
    static int nextBookingId = 1;

    public static void main(String[] args) {

        int choice;

        do {
            System.out.println("\n===== BOOKING & TRIP MANAGEMENT =====");
            System.out.println("1. Book a Ride");
            System.out.println("2. View Booking Details");
            System.out.println("3. Update Scheduled Booking");
            System.out.println("4. Cancel Booking");
            System.out.println("5. Assign Driver");
            System.out.println("6. Driver Accept/Reject Ride");
            System.out.println("7. Update Ride Status");
            System.out.println("8. Start Trip");
            System.out.println("9. Complete Trip");
            System.out.println("10. View Current Trip");
            System.out.println("11. View Booking/Trip History");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");
            choice = input.nextInt();
            input.nextLine();

            switch (choice) {

                case 1:
                    bookRide();
                    break;

                case 2:
                    viewBooking();
                    break;

                case 3:
                    updateBooking();
                    break;

                case 4:
                    cancelBooking();
                    break;

                case 5:
                    assignDriver();
                    break;

                case 6:
                    driverResponse();
                    break;

                case 7:
                    updateRideStatus();
                    break;

                case 8:
                    startTrip();
                    break;

                case 9:
                    completeTrip();
                    break;

                case 10:
                    viewCurrentTrip();
                    break;

                case 11:
                    viewHistory();
                    break;

                case 0:
                    System.out.println("Exiting Booking Management System.");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 0);

        input.close();
    }

    // 1. Book a ride
    static void bookRide() {

        System.out.print("Enter passenger name: ");
        String name = input.nextLine();

        System.out.print("Enter pickup location: ");
        String pickup = input.nextLine();

        System.out.print("Enter destination: ");
        String destination = input.nextLine();

        System.out.println("1. Book now");
        System.out.println("2. Schedule for later");
        System.out.print("Choose booking type: ");

        int type = input.nextInt();
        input.nextLine();

        String time;

        if (type == 1) {
            time = "Now";
        } else {
            System.out.print("Enter scheduled date and time: ");
            time = input.nextLine();
        }

        Booking booking = new Booking(
                nextBookingId++,
                name,
                pickup,
                destination,
                time
        );

        bookings.add(booking);

        System.out.println("Ride booked successfully!");
        System.out.println("Booking ID: " + booking.bookingId);
    }

    // Find a booking using its ID
    static Booking findBooking(int id) {

        for (Booking booking : bookings) {
            if (booking.bookingId == id) {
                return booking;
            }
        }

        return null;
    }

    // 2. View booking details
    static void viewBooking() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking != null) {
            booking.displayDetails();
        } else {
            System.out.println("Booking not found.");
        }
    }

    // 3. Update scheduled booking
    static void updateBooking() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (!booking.status.equals("Pending")) {
            System.out.println("Only pending bookings can be updated.");
            return;
        }

        System.out.print("Enter new pickup location: ");
        booking.pickup = input.nextLine();

        System.out.print("Enter new destination: ");
        booking.destination = input.nextLine();

        System.out.print("Enter new scheduled date and time: ");
        booking.bookingTime = input.nextLine();

        System.out.println("Booking updated successfully.");
    }

    // 4. Cancel booking and store cancellation reason
    static void cancelBooking() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (booking.status.equals("Completed")
                || booking.status.equals("Cancelled")) {
            System.out.println("This booking cannot be cancelled.");
            return;
        }

        System.out.print("Enter cancellation reason: ");
        booking.cancellationReason = input.nextLine();

        booking.status = "Cancelled";

        System.out.println("Booking cancelled successfully.");
    }

    // 5. Assign driver
    static void assignDriver() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (!booking.status.equals("Pending")) {
            System.out.println("Driver cannot be assigned to this booking.");
            return;
        }

        System.out.print("Enter driver name: ");
        booking.driverName = input.nextLine();

        booking.status = "Driver Assigned";

        System.out.println("Driver assigned successfully.");
    }

    // 6. Driver accepts or rejects ride
    static void driverResponse() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (!booking.status.equals("Driver Assigned")) {
            System.out.println("No assigned ride is waiting for a response.");
            return;
        }

        System.out.println("1. Accept ride");
        System.out.println("2. Reject ride");
        System.out.print("Enter choice: ");

        int choice = input.nextInt();
        input.nextLine();

        if (choice == 1) {
            booking.status = "Accepted";
            System.out.println("Ride accepted by driver.");
        } else if (choice == 2) {
            booking.status = "Pending";
            booking.driverName = "Not Assigned";
            System.out.println("Ride rejected. Driver assignment removed.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    // 7. Update ride status
    static void updateRideStatus() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        System.out.print("Enter new ride status: ");
        String status = input.nextLine();

        if (status.equalsIgnoreCase("Pending")
                || status.equalsIgnoreCase("Driver Assigned")
                || status.equalsIgnoreCase("Accepted")
                || status.equalsIgnoreCase("In Progress")
                || status.equalsIgnoreCase("Completed")
                || status.equalsIgnoreCase("Cancelled")) {

            booking.status = status;
            System.out.println("Ride status updated successfully.");

        } else {
            System.out.println("Invalid ride status.");
        }
    }

    // 8. Start trip
    static void startTrip() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (!booking.status.equals("Accepted")) {
            System.out.println("Driver must accept the ride before starting.");
            return;
        }

        booking.status = "In Progress";

        System.out.println("Trip started successfully.");
    }

    // 9. Complete trip
    static void completeTrip() {

        System.out.print("Enter booking ID: ");
        int id = input.nextInt();
        input.nextLine();

        Booking booking = findBooking(id);

        if (booking == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (!booking.status.equals("In Progress")) {
            System.out.println("Trip must be in progress before completion.");
            return;
        }

        booking.status = "Completed";

        System.out.println("Trip completed successfully.");
    }

    // 10. View current trip
    static void viewCurrentTrip() {

        boolean found = false;

        for (Booking booking : bookings) {

            if (booking.status.equals("In Progress")) {
                booking.displayDetails();
                found = true;
            }
        }

        if (!found) {
            System.out.println("There are no trips in progress.");
        }
    }

    // 11. View booking/trip history
    static void viewHistory() {

        if (bookings.isEmpty()) {
            System.out.println("No booking history available.");
            return;
        }

        System.out.println("\n===== BOOKING / TRIP HISTORY =====");

        for (Booking booking : bookings) {
            booking.displayDetails();
        }
    }
}
