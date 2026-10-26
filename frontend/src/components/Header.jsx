import React from 'react';

export default function Header() {
  return (
    <header className="bg-indigo-600 text-white shadow-md w-full">
      <div className="w-full px-4 sm:px-6 lg:px-8 py-2.5 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="text-xl sm:text-2xl">🛒</span>
          <div>
            <h5 className="text-sm sm:text-base font-bold tracking-tight leading-none">TrolleyCompare</h5>
            <p className="text-[10px] sm:text-[11px] text-indigo-200 mt-0.5">Irish Grocery Price Comparison Engine</p>
          </div>
        </div>
      </div>
    </header>
  );
}