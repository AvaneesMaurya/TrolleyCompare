import React from 'react';

export default function ComparisonResults({ loading, comparisonResult }) {
  if (loading) {
    return (
      <div className="bg-white border border-gray-100 rounded-2xl p-12 text-center shadow-sm">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-emerald-500 border-t-transparent mb-3"></div>
        <p className="text-gray-500 font-medium">Comparing prices across Irish supermarkets...</p>
      </div>
    );
  }

  if (!comparisonResult || comparisonResult.length === 0) {
    return (
      <div className="bg-white border border-dashed border-gray-200 rounded-2xl p-12 text-center text-gray-400">
        <p className="text-lg">Add items to your basket and click <span className="font-semibold text-blue-600">"Compare Basket Prices"</span> to see results!</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-gray-700">Supermarket Comparisons</h2>

      {comparisonResult.map((store, index) => {
        const isCheapestOverall = index === 0; // Sorted by cheapest store total

        const regPrice = store.totalRegularPrice;
        const clubPrice = store.totalClubPrice;
        const hasClubDiscount = clubPrice !== null && clubPrice !== undefined && regPrice !== null && clubPrice < regPrice;
        const bestStorePrice = hasClubDiscount ? clubPrice : regPrice;

        return (
          <div 
            key={store.storeName || index}
            className={`bg-white rounded-2xl border transition-all p-6 shadow-sm ${
              isCheapestOverall ? 'border-emerald-500 ring-2 ring-emerald-100' : 'border-gray-100'
            }`}
          >
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
              <div className="flex items-center gap-3">
                <h3 className="text-lg font-bold text-gray-800">{store.storeName}</h3>
                {isCheapestOverall && (
                  <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-2.5 py-1 rounded-full uppercase tracking-wider">
                    🏆 Cheapest Store
                  </span>
                )}
              </div>
              
              {/* Price Summary Box */}
              <div className="text-right bg-gray-50 p-3 rounded-xl border border-gray-100 w-full sm:w-auto">
                <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-0.5">Best Store Total</div>
                <div className="text-2xl font-extrabold text-emerald-600">
                  {bestStorePrice !== null && bestStorePrice !== undefined ? `€${bestStorePrice.toFixed(2)}` : 'N/A'}
                </div>
                <div className="flex items-center justify-end gap-3 mt-1 text-xs">
                  {regPrice !== null && regPrice !== undefined && (
                    <span className="text-gray-500">
                      Regular: <strong className="text-gray-700">€{regPrice.toFixed(2)}</strong>
                    </span>
                  )}
                  {clubPrice !== null && clubPrice !== undefined && (
                    <span className="text-blue-600">
                      Club: <strong className="font-semibold">€{clubPrice.toFixed(2)}</strong>
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Item Breakdown Table with Regular & Club Prices */}
            <div className="border-t pt-3 overflow-x-auto">
              <table className="w-full text-left text-xs text-gray-600">
                <thead>
                  <tr className="text-gray-400 border-b">
                    <th className="pb-2 font-medium">Item</th>
                    <th className="pb-2 font-medium">Store Package</th>
                    <th className="pb-2 font-medium text-right">Regular Price</th>
                    <th className="pb-2 font-medium text-right">Club Price</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {store.items && store.items.map((item, i) => {
                    const isNA = item.productName === 'NA';
                    const itemReg = item.calculatedRegularPrice;
                    const itemClub = item.calculatedClubPrice;
                    const itemHasClubDiscount = itemClub !== null && itemReg !== null && itemClub < itemReg;

                    return (
                      <tr key={i} className={isNA ? 'bg-red-50/50 text-red-400 font-medium' : ''}>
                        <td className="py-2.5 font-medium text-gray-800">{item.productName}</td>
                        <td className="py-2.5 text-gray-500">
                          {item.storePackageQuantity !== null && item.storePackageQuantity !== undefined 
                            ? `${item.storePackageQuantity} ${item.storePackageUnit}` 
                            : 'NA'}
                        </td>
                        <td className="py-2.5 text-right font-semibold text-gray-700">
                          {itemReg !== null && itemReg !== undefined ? `€${itemReg.toFixed(2)}` : '❌ Not Available'}
                        </td>
                        <td className="py-2.5 text-right font-semibold">
                          {itemClub !== null && itemClub !== undefined ? (
                            <span className={itemHasClubDiscount ? 'text-blue-600 font-bold' : 'text-gray-600'}>
                              €{itemClub.toFixed(2)}
                              {itemHasClubDiscount && <span className="text-[10px] ml-1 bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">Club</span>}
                            </span>
                          ) : (
                            <span className="text-gray-400">-</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

          </div>
        );
      })}
    </div>
  );
}