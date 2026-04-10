import { FaTachometerAlt, FaPlus, FaSearch, FaBell, FaList, FaEdit, FaTrash, FaChartLine } from "react-icons/fa";

const menuItems = [
  { key: "dashboard", label: "Dashboard", icon: <FaTachometerAlt /> },
  { key: "add", label: "Add Medicine", icon: <FaPlus /> },
  { key: "search", label: "Search Medicine", icon: <FaSearch /> },
  { key: "update", label: "Update Quantity", icon: <FaEdit /> },
  { key: "remove", label: "Remove Medicine", icon: <FaTrash /> },
  { key: "inventory", label: "View Inventory", icon: <FaList /> },
  { key: "expiry", label: "Expiring Soon", icon: <FaBell /> },
  { key: "low-stock", label: "Low Stock", icon: <FaChartLine /> },
  { key: "alerts", label: "Alert Dashboard", icon: <FaBell /> },
  { key: "stats", label: "Statistics", icon: <FaChartLine /> }
];

export default function Sidebar({ activePage, onSelect }) {
  return (
    <div className="sidebar">
      <h2 className="logo">Medicine Tracker</h2>
      <ul>
        {menuItems.map((item) => (
          <li
            key={item.key}
            className={activePage === item.key ? "active" : ""}
            onClick={() => onSelect(item.key)}
          >
            {item.icon}
            {item.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
