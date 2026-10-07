document.addEventListener("DOMContentLoaded", function () {
    const flashAlerts = document.querySelectorAll(".alert-dismissible");
    flashAlerts.forEach(function (alert) {
        setTimeout(function () {
            try {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            } catch (e) {}
        }, 5000);
    });
    // custom location input like where the item was found 
    const locationSelect = document.getElementById("location_select");
    const customLocationGroup = document.getElementById("custom_location_group");
    const locationCustomInput = document.getElementById("location_custom");
    if (locationSelect && customLocationGroup) {
        function toggleCustomLocation() {
            if (locationSelect.value === "Other") {
                customLocationGroup.style.display = "block";
                if (locationCustomInput) locationCustomInput.required = true;
            } else {
                customLocationGroup.style.display = "none";
                if (locationCustomInput) locationCustomInput.required = false;
            }
        }
        locationSelect.addEventListener("change", toggleCustomLocation);
        toggleCustomLocation();
    }
    //mark notification as read 
    const markReadButtons = document.querySelectorAll(".mark-read-btn");
    markReadButtons.forEach(function (btn) {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            const notifId = this.getAttribute("data-id");
            if (!notifId) return;
            fetch("/notifications/read/" + notifId, {
                method: "POST",
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === "success") {
                    const itemRow = document.getElementById("notif-row-" + notifId);
                    if (itemRow) {
                        itemRow.classList.remove("notif-item-unread");
                        itemRow.classList.add("text-muted");
                    }
                    btn.remove();
                }
            })
            .catch(err => console.error("Notification mark read error:", err));
        });
    });
    // mark all msg as read button 
    const markAllReadBtn = document.getElementById("mark-all-read-btn");
    if (markAllReadBtn) {
        markAllReadBtn.addEventListener("click", function (e) {
            e.preventDefault();
            fetch("/notifications/read-all", {
                method: "POST",
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === "success") {
                    location.reload();
                }
            })
            .catch(err => console.error("Mark all read error:", err));
        });
    }
});
