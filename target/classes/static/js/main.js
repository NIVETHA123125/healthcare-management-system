// --- Live Clock logic ---
function updateClock() {
    const clockElement = document.getElementById('live-clock');
    if (clockElement) {
        const now = new Date();
        let hours = now.getHours();
        let minutes = now.getMinutes();
        const ampm = hours >= 12 ? 'PM' : 'AM';
        
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0' + minutes : minutes;
        
        const timeString = hours + ':' + minutes + ' ' + ampm;
        clockElement.textContent = timeString;
    }
}

// Confirmations
function confirmDeletePatient() {
    return confirm("Are you sure you want to delete this patient? All associated appointments will be permanently removed.");
}

function confirmDeleteDoctor() {
    return confirm("Are you sure you want to delete this doctor? All associated appointments will be permanently removed.");
}

function confirmCancelAppointment() {
    return confirm("Are you sure you want to cancel this appointment?");
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    updateClock();
    setInterval(updateClock, 1000);
});
