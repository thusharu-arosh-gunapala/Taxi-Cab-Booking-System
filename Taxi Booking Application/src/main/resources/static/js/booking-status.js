const statusElement = document.querySelector("#booking-status");
const statusMessage = document.querySelector("#booking-status-message");

if (statusElement && window.bookingEventsUrl && window.EventSource) {
    const source = new EventSource(window.bookingEventsUrl);
    source.addEventListener("booking-status", (event) => {
        const update = JSON.parse(event.data);
        statusElement.textContent = update.status.replaceAll("_", " ");
        statusMessage.textContent = `Booking ${update.bookingReference} is now ${update.status.replaceAll("_", " ").toLowerCase()}.`;
        if (["COMPLETED", "CANCELLED", "NO_DRIVER_AVAILABLE"].includes(update.status)) {
            source.close();
        }
    });
    source.onerror = () => {
        statusMessage.textContent = "Live updates are temporarily disconnected. Refresh to see the latest status.";
        source.close();
    };
}
