import { useState } from "react";

import Dashboard from "./components/Dashboard";
import Sidebar from "./components/Sidebar";

function App() {

  const [medicines, setMedicines] = useState([
    {
      id: 1,
      name: "Ibuprofen",
      quantity: 5,
      expiry: "2024-08-10"
    },
    {
      id: 2,
      name: "Ciprofloxacin",
      quantity: 30,
      expiry: "2024-08-12"
    }
  ]);

  return (
    <Dashboard
      medicines={medicines}
      setMedicines={setMedicines}
    />
  );
}

export default App;