import { useState } from "react";

export default function SearchSection({ medicines }) {

  const [query, setQuery] = useState("");
  const [result, setResult] = useState(null);

  const searchMedicine = () => {

    const med = medicines.find(
      m => m.name.toLowerCase() === query.toLowerCase()
    );

    setResult(med);
  };

  return (
    <div>

      <h2>Search Medicine</h2>

      <input
        value={query}
        onChange={(e)=>setQuery(e.target.value)}
      />

      <button onClick={searchMedicine}>
        Search
      </button>

      {result && (
        <div>
          <h3>{result.name}</h3>
          <p>Qty: {result.quantity}</p>
          <p>Expiry: {result.expiry}</p>
        </div>
      )}

    </div>
  );
}