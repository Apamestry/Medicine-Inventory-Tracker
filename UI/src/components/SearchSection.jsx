import { useState } from "react";

export default function SearchSection({ onSearch, result }) {
  const [query, setQuery] = useState("");

  return (
    <div className="box">
      <h2>Search Medicine</h2>
      <div className="form-row">
        <input
          value={query}
          placeholder="Medicine name"
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>
      <button onClick={() => onSearch(query)}>Search</button>
      {result ? (
        <div className="box result-card">
          <h3>{result.name}</h3>
          <p>Batch: {result.batchNo}</p>
          <p>Qty: {result.quantity}</p>
          <p>Expiry: {result.expiry}</p>
        </div>
      ) : (
        query && <p>No medicine found for "{query}".</p>
      )}
    </div>
  );
}
