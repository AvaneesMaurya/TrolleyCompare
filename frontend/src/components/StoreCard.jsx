import React from 'react';

export default function StoreCard({ store, cheapestStoreName }) {
  const isCheapest = store.storeName === cheapestStoreName;

  return (
    <div className="bg-white p-3 rounded-xl shadow-sm border border-gray-100 flex flex-col justify-between">
      <div>
        <div className="flex justify-between items-center mb-2">
          <h3 className="font-bold text-xs text-gray-900">{store.storeName}</h3>
          <span className={`text-[9px] px-1.5 py-0.5 rounded-full font-semibold ${isCheapest ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
            {isCheapest ? '🏆 Cheapest' : 'Store Option'}
          </span>
        </div>
        
        {/* Item Breakdown */}
        <div className="space-y-1 mb-2.5">
          {store.items.map((item, idx) => (
            <div key={idx} className="flex justify-between items-center text-[11px] border-b border-gray-50 pb-0.5 gap-1">
              <span className="text-gray-600 truncate">{item.productName} (x{item.quantity})</span>
              <span className={`font-medium whitespace-nowrap ${item.regularSubtotal !== null ? 'text-gray-900' : 'text-red-400 font-semibold'}`}>
                {item.regularSubtotal !== null ? '€' + item.regularSubtotal.toFixed(2) : 'N/A'}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Store Total Footer */}
      <div className="border-t border-gray-100 pt-2 flex justify-between items-center bg-gray-50 -mx-3 -mb-3 p-2 rounded-b-xl gap-1">
        <div>
          <span className="text-[9px] text-gray-500 block">Regular Total</span>
          <div className="text-xs sm:text-sm font-extrabold text-indigo-600">€{(store.totalRegularPrice || 0).toFixed(2)}</div>
        </div>
        {store.totalClubPrice !== null && store.totalClubPrice !== store.totalRegularPrice && (
          <div className="text-right">
            <span className="text-[9px] text-gray-500 block">Club Total</span>
            <div className="text-xs sm:text-sm font-extrabold text-emerald-600">€{store.totalClubPrice.toFixed(2)}</div>
          </div>
        )}
      </div>
    </div>
  );
}