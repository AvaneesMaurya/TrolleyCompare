import React from 'react';

export default function BasketList({ basketItems, onRemoveItem, onClearBasket, onCompare }) {
  if (!basketItems || basketItems.length === 0) {
    return null;
  }

  return (
    <div className="bg-white p-3 rounded-xl shadow-sm border border-gray-100 flex flex-col justify-between space-y-2.5">
      <div>
        <div className="flex justify-between items-center mb-2 gap-1.5 flex-nowrap">
          <h2 className="text-[11px] sm:text-xs font-bold text-gray-900 truncate tracking-tight">
            🛍️ Grocery List ({basketItems.length})
          </h2>
          <button 
            onClick={onClearBasket} 
            title="Clear list"
            className="text-red-400 hover:text-red-600 p-0.5 rounded hover:bg-red-50 transition flex-shrink-0"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>

        <div className="space-y-1 max-h-60 overflow-y-auto pr-1">
          {basketItems.map((item, idx) => (
            <div key={idx} className="flex justify-between items-center bg-gray-50 p-1.5 rounded-md border text-[11px]">
              <span className="font-medium text-gray-700 truncate mr-1">
                {item.productName} ({item.quantity} {item.unit})
              </span>
              <button 
                onClick={() => onRemoveItem(idx)} 
                className="text-red-500 hover:text-red-700 font-bold text-[10px] px-1 py-0.5 flex-shrink-0"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      <button 
        onClick={onCompare} 
        className="w-full bg-green-600 text-white py-2 rounded-lg font-bold hover:bg-green-700 transition shadow-sm text-[11px]"
      >
        Compare Supermarkets
      </button>
    </div>
  );
}