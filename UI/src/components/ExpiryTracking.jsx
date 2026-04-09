import { getDaysLeft } from "../utils/alertSystem";

export default function ExpiryTracking({ medicines }) {

  const sorted = [...medicines].sort(
    (a, b) => new Date(a.expiry) - new Date(b.expiry)
  );

  return (
    <div className="box">

      <h2>Expiry Tracking</h2>

      <ul>
        {sorted.map(m => {
          const days = getDaysLeft(m.expiry);

          return (
            <li key={m.id}>
              {m.name} - {days} days left
            </li>
          );
        })}
      </ul>

    </div>
  );
}