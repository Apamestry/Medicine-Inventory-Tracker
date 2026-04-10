import { useState } from "react";

export default function AddMedicine({ onAdd }) {
  const [name, setName] = useState("");
  const [batchNo, setBatchNo] = useState("");
  const [qty, setQty] = useState("");
  const [expiry, setExpiry] = useState("");

  return (
    <div className="box">
      <h2>Add Medicine</h2>
      <div className="form-row">
        <input
          placeholder="Medicine name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div className="form-row">
        <input
          placeholder="Batch number"
          value={batchNo}
          onChange={(e) => setBatchNo(e.target.value)}
        />
      </div>
      <div className="form-row">
        <input
          type="number"
          placeholder="Quantity"
          value={qty}
          onChange={(e) => setQty(e.target.value)}
        />
      </div>
      <div className="form-row">
        <input
          type="date"
          value={expiry}
          onChange={(e) => setExpiry(e.target.value)}
        />
      </div>
      <button
        onClick={() =>
          onAdd({
            name,
            batchNo,
            quantity: Number(qty),
            expiry
          })
        }
      >
        Add Medicine
      </button>
    </div>
  );
}
