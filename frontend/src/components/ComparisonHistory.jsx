import { useCallback, useEffect, useState } from 'react';
import { requestApi } from '../services/api';

export default function ComparisonHistory({ onViewComparison }) {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const loadHistory = useCallback(async () => {
    const records = await requestApi('/history');
    setHistory(records);
  }, []);

  useEffect(() => {
    let active = true;
    const fetchHistory = async () => {
      try {
        await loadHistory();
      } catch (requestError) {
        if (active) setError(requestError.message);
      } finally {
        if (active) setLoading(false);
      }
    };
    fetchHistory();
    return () => { active = false; };
  }, [loadHistory]);

  if (loading) {
    return <p className="py-8 text-center text-sm text-gray-500">Loading comparison history…</p>;
  }

  return (
    <section className="space-y-5 text-left">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Comparison history</h1>
        <p className="mt-1 text-sm text-gray-500">Your saved price comparisons.</p>
      </div>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {!error && history.length === 0 && (
        <div className="rounded-xl border border-dashed border-gray-300 bg-white p-8 text-center text-sm text-gray-500">
          No saved comparisons yet. Compare a basket while signed in and it will appear here.
        </div>
      )}
      <div className="space-y-4">
        {history.map((entry) => (
          <article key={entry.id} className="rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="font-semibold text-gray-900">
                  {new Date(entry.createdAt).toLocaleString()}
                </h2>
                <p className="mt-1 text-sm text-gray-600">
                  {entry.items.map((item) => `${item.name} (${item.quantity} ${item.unit})`).join(', ')}
                </p>
              </div>
              <button
                onClick={() => onViewComparison(entry)}
                className="rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
              >
                View results
              </button>
            </div>
            <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {entry.results.map((store) => (
                <div key={store.storeName} className="rounded-lg bg-gray-50 p-3 text-sm">
                  <p className="font-semibold text-gray-800">{store.storeName}</p>
                  <p className="mt-1 text-gray-600">Regular: €{store.totalRegularPrice.toFixed(2)}</p>
                  <p className="text-gray-600">Club: €{store.totalClubPrice.toFixed(2)}</p>
                </div>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
