import { useEffect, useState } from "react";

import Sidebar from "./components/Sidebar";
import AddMedicine from "./components/AddMedicine";
import SearchSection from "./components/SearchSection";
import ExpiryTracking from "./components/ExpiryTracking";
import Alerts from "./components/Alerts";
import Overview from "./components/Overview";
import "./styles.css";

const API_BASE = "http://localhost:8082/api";

function App() {
  const [activePage, setActivePage] = useState("dashboard");
  const [medicines, setMedicines] = useState([]);
  const [stats, setStats] = useState({});
  const [alertsData, setAlertsData] = useState(null);
  const [searchResult, setSearchResult] = useState(null);
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    loadDashboard();
  }, [refreshTrigger]);

  async function callApi(path, options = {}) {
    try {
      const response = await fetch(`${API_BASE}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
      });
      const text = await response.text();
      if (!response.ok) {
        throw new Error(text || "Backend request failed");
      }
      return text ? JSON.parse(text) : null;
    } catch (error) {
      console.error(error);
      throw error;
    }
  }

  async function loadDashboard() {
    setIsLoading(true);
    setMessage("");
    try {
      const [items, statsPayload, alertsPayload] = await Promise.all([
        callApi("/medicines"),
        callApi("/stats"),
        callApi("/alerts")
      ]);
      setMedicines(items || []);
      setStats(statsPayload || {});
      setAlertsData(alertsPayload || {});
    } catch (error) {
      setMessage("Unable to connect to the backend. Start the Java server and refresh.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleAddMedicine(medicine) {
    setMessage("");
    try {
      await callApi("/medicines", {
        method: "POST",
        body: JSON.stringify(medicine)
      });
      setMessage("Medicine added successfully.");
      setRefreshTrigger((count) => count + 1);
      setActivePage("dashboard");
    } catch (error) {
      setMessage("Add medicine failed. Check input and backend status.");
    }
  }

  async function handleSearch(query) {
    if (!query) {
      setSearchResult(null);
      return;
    }
    try {
      const result = await callApi(`/medicines/search?name=${encodeURIComponent(query)}`);
      setSearchResult(result);
    } catch (error) {
      setMessage("Search failed. Check backend status.");
    }
  }

  async function handleUpdateQuantity(name, quantity) {
    setMessage("");
    try {
      await callApi("/medicines/quantity", {
        method: "PUT",
        body: JSON.stringify({ name, quantity })
      });
      setMessage("Quantity updated successfully.");
      setRefreshTrigger((count) => count + 1);
      setActivePage("dashboard");
    } catch (error) {
      setMessage("Quantity update failed. Verify the medicine name and try again.");
    }
  }

  async function handleRemoveMedicine(name) {
    setMessage("");
    try {
      await callApi(`/medicines?name=${encodeURIComponent(name)}`, {
        method: "DELETE"
      });
      setMessage("Medicine removed successfully.");
      setRefreshTrigger((count) => count + 1);
      setActivePage("dashboard");
    } catch (error) {
      setMessage("Removal failed. Verify the medicine name and try again.");
    }
  }

  function renderPage() {
    if (isLoading) {
      return <div className="box">Loading backend data...</div>;
    }

    switch (activePage) {
      case "dashboard":
        return (
          <>
            <Overview medicines={medicines} />
            <div className="grid">
              <div className="box">
                <h2>Backend Alerts Snapshot</h2>
                <p>Expired: {alertsData?.expired?.length ?? 0}</p>
                <p>Expiring Soon: {alertsData?.expiringSoon?.length ?? 0}</p>
                <p>Low Stock: {alertsData?.lowStock?.length ?? 0}</p>
              </div>
              <div className="box">
                <h2>Quick Stats</h2>
                <p>Total Medicines: {stats.totalMedicines ?? 0}</p>
                <p>Total Stock: {stats.totalStockUnits ?? 0}</p>
                <p>Next Expiry: {stats.nextExpiryDate ?? "N/A"}</p>
              </div>
            </div>
          </>
        );
      case "add":
        return <AddMedicine onAdd={handleAddMedicine} />;
      case "search":
        return <SearchSection onSearch={handleSearch} result={searchResult} />;
      case "update":
        return <UpdateQuantityForm onUpdate={handleUpdateQuantity} />;
      case "remove":
        return <RemoveMedicineForm onRemove={handleRemoveMedicine} />;
      case "inventory":
        return <InventoryTable medicines={medicines} />;
      case "expiry":
        return <ExpiryTracking medicines={medicines} />;
      case "low-stock":
        return <Alerts medicines={medicines} />;
      case "alerts":
        return <AlertsDashboard alerts={alertsData} />;
      case "stats":
        return <StatsView stats={stats} />;
      default:
        return <div className="box">Select a menu item to begin.</div>;
    }
  }

  return (
    <div className="app">
      <Sidebar activePage={activePage} onSelect={setActivePage} />
      <main className="dashboard">
        <header className="main-header">
          <h1>Medicine Inventory Tracker</h1>
          <p>Select an action from the sidebar to control the inventory.</p>
        </header>

        {message && <div className="alert-banner">{message}</div>}
        {renderPage()}
      </main>
    </div>
  );
}

function InventoryTable({ medicines }) {
  return (
    <div className="box">
      <h2>Inventory List</h2>
      <table>
        <thead>
          <tr>
            <th>Batch</th>
            <th>Name</th>
            <th>Qty</th>
            <th>Expiry</th>
          </tr>
        </thead>
        <tbody>
          {medicines.map((med, index) => (
            <tr key={`${med.name}-${index}`}>
              <td>{med.batchNo}</td>
              <td>{med.name}</td>
              <td>{med.quantity}</td>
              <td>{med.expiry}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function UpdateQuantityForm({ onUpdate }) {
  const [name, setName] = useState("");
  const [quantity, setQuantity] = useState("");

  return (
    <div className="box">
      <h2>Update Quantity</h2>
      <div className="form-row">
        <input
          value={name}
          placeholder="Medicine name"
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div className="form-row">
        <input
          type="number"
          value={quantity}
          placeholder="New quantity"
          onChange={(e) => setQuantity(e.target.value)}
        />
      </div>
      <button onClick={() => onUpdate(name, Number(quantity))}>
        Update Quantity
      </button>
    </div>
  );
}

function RemoveMedicineForm({ onRemove }) {
  const [name, setName] = useState("");

  return (
    <div className="box">
      <h2>Remove Medicine</h2>
      <div className="form-row">
        <input
          value={name}
          placeholder="Medicine name"
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <button onClick={() => onRemove(name)}>
        Remove Medicine
      </button>
    </div>
  );
}

function AlertsDashboard({ alerts }) {
  return (
    <div className="box">
      <h2>Alert Dashboard</h2>
      <div className="grid">
        <div className="card">
          <h3>Expired</h3>
          <p>{alerts?.expired?.length ?? 0} medicines</p>
        </div>
        <div className="card">
          <h3>Expiring Soon</h3>
          <p>{alerts?.expiringSoon?.length ?? 0} medicines</p>
        </div>
        <div className="card">
          <h3>Low Stock</h3>
          <p>{alerts?.lowStock?.length ?? 0} medicines</p>
        </div>
      </div>
    </div>
  );
}

function StatsView({ stats }) {
  return (
    <div className="box">
      <h2>Inventory Statistics</h2>
      <div className="grid">
        <div className="box">
          <p>Total Medicines</p>
          <h3>{stats.totalMedicines ?? 0}</h3>
        </div>
        <div className="box">
          <p>Total Stock Units</p>
          <h3>{stats.totalStockUnits ?? 0}</h3>
        </div>
        <div className="box">
          <p>Next Expiry</p>
          <h3>{stats.nextExpiryDate ?? "N/A"}</h3>
        </div>
        <div className="box">
          <p>Latest Expiry</p>
          <h3>{stats.latestOnRecord ?? "N/A"}</h3>
        </div>
      </div>
    </div>
  );
}

export default App;
