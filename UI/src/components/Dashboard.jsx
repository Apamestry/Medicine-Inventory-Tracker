import { useEffect, useRef } from "react";
import { getAlerts } from "../utils/alertSystem";

import Overview from "./Overview";
import SearchSection from "./SearchSection";
import ExpiryTracking from "./ExpiryTracking";
import Alerts from "./Alerts";
import AddMedicine from "./AddMedicine";

export default function Dashboard({ medicines, setMedicines }) {


  const shownNotifications = useRef(new Set());


  useEffect(() => {
    if ("Notification" in window && Notification.permission !== "granted") {
      Notification.requestPermission();
    }
  }, []);


  const alerts = getAlerts(medicines);


  useEffect(() => {
    if (!("Notification" in window)) return;

    if (Notification.permission === "granted") {
      alerts.forEach(a => {
        if (!shownNotifications.current.has(a.message)) {
          new Notification(a.message);
          shownNotifications.current.add(a.message);
        }
      });
    }
  }, [alerts]);

  return (
    <div className="dashboard">

      <h1>Dashboard</h1>

      {/* 🚨 ALERT BANNER */}
      {alerts.length > 0 && (
        <div className="alert-banner">
          {alerts.map((a, i) => (
            <p key={i}>{a.message}</p>
          ))}
        </div>
      )}

      {/* 📊 OVERVIEW */}
      <Overview medicines={medicines} />

      {/* ➕ ADD MEDICINE */}
      <AddMedicine
        medicines={medicines}
        setMedicines={setMedicines}
      />

      {/* 🔍 SEARCH */}
      <SearchSection medicines={medicines} />

      {/* ⏳ EXPIRY */}
      <ExpiryTracking medicines={medicines} />

      {/* ⚠️ ALERTS LIST */}
      <Alerts medicines={medicines} />

    </div>
  );
}