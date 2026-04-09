import { useState } from "react";

export default function AddMedicine({ medicines, setMedicines }) {

  const [name,setName] = useState("");
  const [qty,setQty] = useState("");
  const [expiry,setExpiry] = useState("");

  const addMedicine = () => {

    const newMed = {
      id: Date.now(),
      name,
      quantity: Number(qty),
      expiry
    };

    setMedicines([...medicines,newMed]);

    setName("");
    setQty("");
    setExpiry("");
  };

  return (

    <div className="box">

      <h2>Add Medicine</h2>

      <input
        placeholder="Medicine name"
        value={name}
        onChange={(e)=>setName(e.target.value)}
      />

      <input
        type="number"
        placeholder="Quantity"
        value={qty}
        onChange={(e)=>setQty(e.target.value)}
      />

      <input
        type="date"
        value={expiry}
        onChange={(e)=>setExpiry(e.target.value)}
      />

      <button onClick={addMedicine}>
        Add Medicine
      </button>

    </div>
  );
}