import { useState } from 'react';

const UNIT_OPTIONS = ['L', 'ml', 'kg', 'g', 'pcs', 'pack', 'bottle'];
const COMMON_ITEMS = [
  { name: 'Milk', quantity: 1, unit: 'L' },
  { name: 'Bread', quantity: 1, unit: 'pcs' },
  { name: 'Eggs', quantity: 6, unit: 'pcs' },
  { name: 'Apples', quantity: 1, unit: 'kg' },
  { name: 'Potatoes', quantity: 1, unit: 'kg' }
];

export default function AddProductForm({ onAddItem, recentSearches = [] }) {
  const [itemName, setItemName] = useState('');
  const [itemQty, setItemQty] = useState(1);
  const [itemUnit, setItemUnit] = useState('L');
  const [showSuggestions, setShowSuggestions] = useState(false);

  const matchingRecentSearches = recentSearches
    .filter((item) => item.name.toLowerCase().includes(itemName.trim().toLowerCase()))
    .slice(0, 5);
  const matchingCommonItems = COMMON_ITEMS
    .filter((item) => item.name.toLowerCase().includes(itemName.trim().toLowerCase()));
  const suggestions = matchingRecentSearches.length > 0
    ? matchingRecentSearches
    : matchingCommonItems.length > 0 ? matchingCommonItems : COMMON_ITEMS;
  const showingCommonItems = matchingRecentSearches.length === 0;

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
        <div className="relative w-full">
          <label className="block text-[9px] font-bold uppercase tracking-wider text-gray-500 mb-0.5">
            Product Keyword
          </label>
          <input
            type="text" 
            value={itemName} 
            onChange={(e) => {
              setItemName(e.target.value);
              setShowSuggestions(true);
            }}
            onFocus={() => setShowSuggestions(true)}
            onBlur={() => window.setTimeout(() => setShowSuggestions(false), 150)}
            autoComplete="off"
            placeholder="e.g. Milk, Bread" 
            className="w-full h-8 px-2.5 border border-gray-300 rounded-lg text-[11px] text-gray-800 placeholder-gray-400 bg-gray-50/50 outline-none focus:bg-white focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition box-border" 
          />
          {showSuggestions && suggestions.length > 0 && (
            <ul className="absolute z-20 mt-1 max-h-52 w-full overflow-auto rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
              {showingCommonItems && (
                <li className="px-3 py-1 text-[9px] font-bold uppercase tracking-wider text-gray-400">
                  Common items
                </li>
              )}
              {suggestions.map((suggestion) => (
                <li key={`${suggestion.name}-${suggestion.quantity}-${suggestion.unit}`}>
                  <button
                    type="button"
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => {
                      setItemName(suggestion.name);
                      setItemQty(suggestion.quantity);
                      setItemUnit(suggestion.unit);
                      setShowSuggestions(false);
                    }}
                    className="w-full px-3 py-2 text-left text-xs text-gray-700 hover:bg-indigo-50"
                  >
                    {suggestion.name} <span className="text-gray-400">({suggestion.quantity} {suggestion.unit})</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
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