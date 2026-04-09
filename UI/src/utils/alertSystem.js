
export const getLowStock = (medicines, threshold = 10) => {
  return medicines.filter(m => m.quantity < threshold);
};


export const getDaysLeft = (expiryDate) => {
  const today = new Date();
  const expiry = new Date(expiryDate);

  const diff = expiry - today;
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};


export const getNearExpiry = (medicines, daysLimit = 30) => {
  return medicines.filter(m => getDaysLeft(m.expiry) <= daysLimit);
};

export const getAlerts = (medicines) => {
  const alerts = [];

  medicines.forEach(m => {
    const daysLeft = getDaysLeft(m.expiry);

    if (m.quantity < 10) {
      alerts.push({
        type: "LOW_STOCK",
        message: `${m.name} is low on stock`,
        data: m
      });
    }

    if (daysLeft <= 30) {
      alerts.push({
        type: "EXPIRY",
        message: `${m.name} expires in ${daysLeft} days`,
        data: m
      });
    }
  });

  return alerts;
};