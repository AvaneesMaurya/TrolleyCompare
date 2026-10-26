import React, { useState } from 'react';

const UNIT_OPTIONS = ['L', 'ml', 'kg', 'g', 'pcs', 'pack', 'bottle'];

export default function AddProductForm({ onAddItem }) {
  const [itemName, setItemName] = useState('');
  const [itemQty, setItemQty] = useState(1);
  const [itemUnit, setItemUnit] = useState('L');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!itemName.trim()) return;
    onAddItem(itemName.trim(), parseFloat(itemQty), itemUnit);
    setItemName('');
    setItemQty(1);
  };

  return (
    <div className="bg-white p-3 sm:p-3.5 rounded-xl shadow-sm border border-gray-100 w-full space-y-2">
      <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-4 gap-2 items-end w-full">
        
        {/* Box 1: Product Keyword */}
        <div className="w-full">
          <label className="block text-[9px] font-bold uppercase tracking-wider text-gray-500 mb-0.5">
            Product Keyword
          </label>
          <input 
            type="text" 
            value={itemName} 
            onChange={(e) => setItemName(e.target.value)} 
            placeholder="e.g. Milk, Bread" 
            className="w-full h-8 px-2.5 border border-gray-300 rounded-lg text-[11px] text-gray-800 placeholder-gray-400 bg-gray-50/50 outline-none focus:bg-white focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition box-border" 
          />
        </div>

        {/* Box 2: Quantity */}
        <div className="w-full">
          <label className="block text-[9px] font-bold uppercase tracking-wider text-gray-500 mb-0.5">
            Quantity
          </label>
          <input 
            type="number" 
            value={itemQty} 
            onChange={(e) => setItemQty(e.target.value)} 
            min="1" 
            className="w-full h-8 px-2.5 border border-gray-300 rounded-lg text-[11px] text-gray-800 bg-gray-50/50 outline-none focus:bg-white focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition box-border" 
          />
        </div>

        {/* Box 3: Unit */}
        <div className="w-full">
          <label className="block text-[9px] font-bold uppercase tracking-wider text-gray-500 mb-0.5">
            Unit
          </label>
          <select 
            value={itemUnit} 
            onChange={(e) => setItemUnit(e.target.value)} 
            className="w-full h-8 px-2.5 border border-gray-300 rounded-lg text-[11px] text-gray-800 bg-gray-50/50 outline-none focus:bg-white focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition box-border cursor-pointer">
            {UNIT_OPTIONS.map((u) => (
              <option key={u} value={u}>{u}</option>
            ))}
          </select>
        </div>

        {/* Action Button */}
        <div className="w-full">
          <button 
            type="submit" 
            className="w-full h-8 bg-indigo-600 text-white rounded-lg font-bold hover:bg-indigo-700 transition text-[11px] shadow-sm whitespace-nowrap flex items-center justify-center"
          >
            + Add Item
          </button>
        </div>
      </form>
    </div>
  );
}