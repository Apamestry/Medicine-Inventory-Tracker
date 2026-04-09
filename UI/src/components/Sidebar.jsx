import { FaTachometerAlt, FaPlus, FaSearch, FaBell, FaList } from "react-icons/fa";

export default function Sidebar() {
  return (

    <div className="sidebar">

      <h2 className="logo">Medicine Tracker</h2>

      <ul>

        <li>
          <FaTachometerAlt /> Dashboard
        </li>

        <li>
          <FaPlus /> Add Medicine
        </li>

        <li>
          <FaSearch /> Search Medicine
        </li>

        <li>
          <FaBell /> Expiry Alerts
        </li>

        <li>
          <FaList /> Inventory List
        </li>

      </ul>

    </div>

  );
}