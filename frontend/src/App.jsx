import React, { useState } from 'react';
import Header from './components/Header';
import AddProductForm from './components/AddProductForm';
import BasketList from './components/BasketList';
import ComparisonResults from './components/ComparisonResults';
// Import the updated query
import { callGraphql, COMPARE_BASKET_QUERY } from './services/api';

export default function App() {
  const [basketItems, setBasketItems] = useState([]);
  const [comparisonResult, setComparisonResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleAddItem = (productName, quantity, unit) => {
    setBasketItems((prev) => [...prev, { productName, quantity, unit }]);
  };

  const handleRemoveItem = (index) => {
    setBasketItems((prev) => {
      const updated = prev.filter((_, i) => i !== index);
      if (updated.length === 0) setComparisonResult(null);
      return updated;
    });
  };

  const handleClearBasket = () => {
    setBasketItems([]);
    setComparisonResult(null);
  };

  const runComparison = async () => {
    if (basketItems.length === 0) return;

    setLoading(true);
    setComparisonResult(null);

    try {
      // Pass items directly to match the backend [ItemInput!]! argument
      const data = await callGraphql(COMPARE_BASKET_QUERY, {
        items: basketItems.map(item => ({
          name: item.productName,
          quantity: Number(item.quantity),
          unit: item.unit
        }))
      });
      setComparisonResult(data.compareBasket);
    } catch (err) {
      // Error handled
    } finally {
      setLoading(false);
    }
  };

  const hasItems = basketItems.length > 0;

  return (
    <div className="bg-gray-50 font-sans min-h-screen text-gray-800 flex flex-col w-full">
      <Header />
      <main className="flex-grow w-full px-4 sm:px-6 lg:px-8 py-4 space-y-4">
        <section className="w-full">
          <AddProductForm onAddItem={handleAddItem} />
        </section>
        <section className="w-full">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-6 items-start w-full">
            {hasItems && (
              <div className="md:col-span-4 w-full">
                <BasketList 
                  basketItems={basketItems} 
                  onRemoveItem={handleRemoveItem} 
                  onClearBasket={handleClearBasket}
                  onCompare={runComparison} 
                />
              </div>
            )}
            <div className={`w-full ${hasItems ? 'md:col-span-8' : 'md:col-span-12'}`}>
              <ComparisonResults 
                loading={loading} 
                comparisonResult={comparisonResult} 
              />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}