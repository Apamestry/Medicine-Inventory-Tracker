export default function Alerts({ medicines }) {
  const threshold = 20;
  const lowStock = medicines.filter((med) => med.quantity < threshold);

  return (
    <div className="alerts">
      <h2>Low Stock Reports</h2>
      <table>
        <thead>
          <tr>
            <th>Medicine</th>
            <th>Quantity</th>
          </tr>
        </thead>
        <tbody>
          {lowStock.map((med, index) => (
            <tr key={`${med.name}-${index}`}>
              <td>{med.name}</td>
              <td>{med.quantity}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export const getNearExpiry = (medicines) => {
  const today = new Date();
  return medicines.filter((med) => {
    const expiryDate = new Date(med.expiry);
    const diff = (expiryDate - today) / (1000 * 60 * 60 * 24);
    return diff <= 30;
  });
};
