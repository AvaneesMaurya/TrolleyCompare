import { useState } from 'react';

const UNIT_OPTIONS = ['L', 'ml', 'kg', 'g', 'pcs', 'pack', 'bottle'];

export default function BasketBuilder({ basketItems, setBasketItems, onCompare }) {
  const [itemName, setItemName] = useState('');
  const [itemQty, setItemQty] = useState(1);
  const [itemUnit, setItemUnit] = useState('L');

  const addQuickItem = (name, qty, unit) => {
    setBasketItems((prev) => [...prev, { productName: name, quantity: qty, unit }]);
  };

  const handleAddCustomItem = (e) => {
    e.preventDefault();
    if (!itemName.trim()) return;
    setBasketItems((prev) => [
      ...prev,
      { productName: itemName.trim(), quantity: parseFloat(itemQty), unit: itemUnit }
    ]);
    setItemName('');
    setItemQty(1);
  };

  const removeItem = (index) => {
    setBasketItems((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="bg-white p-4 sm:p-6 rounded-xl shadow-sm border border-gray-100 lg:col-span-1 space-y-4">
      <h2 className="text-base sm:text-lg font-bold text-gray-900 flex justify-between items-center">
        <span>🛍️ Shopping Basket</span>
        <button onClick={() => setBasketItems([])} className="text-xs text-red-500 hover:underline">
          Clear
        </button>
      </h2>

      <form onSubmit={handleAddCustomItem} className="space-y-3">
        <div>
          <label className="block text-xs font-semibold uppercase text-gray-500 mb-1">Product Keyword</label>
          <input 
            type="text" 
            value={itemName} 
            onChange={(e) => setItemName(e.target.value)} 
            placeholder="e.g. Milk, Bread, Eggs" 
            className="w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500" 
          />
        </div>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="block text-xs font-semibold uppercase text-gray-500 mb-1">Qty</label>
            <input 
              type="number" 
              value={itemQty} 
              onChange={(e) => setItemQty(e.target.value)} 
              min="1" 
              className="w-full px-3 py-2.5 border rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500" 
            />
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase text-gray-500 mb-1">Unit</label>
            <select 
              value={itemUnit} 
              onChange={(e) => setItemUnit(e.target.value)} 
              className="w-full px-3 py-2.5 border rounded-lg text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500">
              {UNIT_OPTIONS.map((u) => (
                <option key={u} value={u}>{u}</option>
              ))}
            </select>
          </div>
        </div>
        <button type="submit" className="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-medium hover:bg-indigo-700 transition text-sm shadow-sm">
          Add Item
        </button>
      </form>

      {/* Quick Suggestions */}
      <div>
        <p className="text-xs font-semibold text-gray-400 uppercase mb-2">Quick Add:</p>
        <div className="flex flex-wrap gap-1.5">
          <button type="button" onClick={() => addQuickItem('Milk', 1, 'L')} className="bg-gray-100 hover:bg-gray-200 text-xs px-2.5 py-1.5 rounded-full transition">🥛 Milk</button>
          <button type="button" onClick={() => addQuickItem('Bread', 1, 'pcs')} className="bg-gray-100 hover:bg-gray-200 text-xs px-2.5 py-1.5 rounded-full transition">🍞 Bread</button>
          <button type="button" onClick={() => addQuickItem('Potatoes', 1, 'kg')} className="bg-gray-100 hover:bg-gray-200 text-xs px-2.5 py-1.5 rounded-full transition">🥔 Potatoes</button>
          <button type="button" onClick={() => addQuickItem('Eggs', 1, 'pcs')} className="bg-gray-100 hover:bg-gray-200 text-xs px-2.5 py-1.5 rounded-full transition">🥚 Eggs</button>
        </div>
      </div>

      <hr className="border-gray-100" />

      {/* Basket Items List */}
      <div>
        <h3 className="text-xs font-semibold uppercase text-gray-500 mb-2">Items in Basket ({basketItems.length})</h3>
        <div className="space-y-2 max-h-48 sm:max-h-60 overflow-y-auto pr-1">
          {basketItems.length === 0 ? (
            <p className="text-xs text-gray-400 italic py-2">Your basket is empty. Add items above.</p>
          ) : (
            basketItems.map((item, idx) => (
              <div key={idx} className="flex justify-between items-center bg-gray-50 p-2.5 rounded-lg border text-sm">
                <span className="font-medium text-gray-700 truncate mr-2">{item.productName} ({item.quantity} {item.unit})</span>
                <button onClick={() => removeItem(idx)} className="text-red-500 hover:text-red-700 font-bold text-xs px-2 py-1 flex-shrink-0">✕</button>
              </div>
            ))
          )}
        </div>
      </div>

      <button 
        onClick={onCompare} 
        className="w-full bg-green-600 text-white py-3 rounded-lg font-bold hover:bg-green-700 transition shadow-sm text-sm">
        Compare Supermarkets 🚀
      </button>
    </div>
  );
}