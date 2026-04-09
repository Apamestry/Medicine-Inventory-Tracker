import { getLowStock, getNearExpiry } from "../utils/alertSystem";

export default function Overview({ medicines }) {

  const lowStock = getLowStock(medicines);
  const expiry = getNearExpiry(medicines);

  return (
    <div className="overview">

      <div className="card">
        Total Medicines
        <h2>{medicines.length}</h2>
      </div>

      <div className="card">
        Low Stock
        <h2>{lowStock.length}</h2>
      </div>

      <div className="card">
        Near Expiry
        <h2>{expiry.length}</h2>
      </div>

    </div>
  );
}