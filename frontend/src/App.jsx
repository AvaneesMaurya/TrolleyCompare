import { useEffect, useState } from 'react';
import Header from './components/Header';
import AddProductForm from './components/AddProductForm';
import BasketList from './components/BasketList';
import ComparisonResults from './components/ComparisonResults';
import AuthPage from './components/AuthPage';
import UserManagement from './components/UserManagement';
import UserProfile from './components/UserProfile';
import ComparisonHistory from './components/ComparisonHistory';
// Import the updated query
import { callGraphql, COMPARE_BASKET_QUERY, refreshCsrfToken, requestApi } from './services/api';

export default function App() {
  const [user, setUser] = useState(null);
  const [isGuest, setIsGuest] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [authError, setAuthError] = useState('');
  const [activeView, setActiveView] = useState('basket');
  const [basketItems, setBasketItems] = useState([]);
  const [recentSearches, setRecentSearches] = useState([]);
  const [comparisonResult, setComparisonResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [comparisonError, setComparisonError] = useState('');

  useEffect(() => {
    let active = true;
    const loadSession = async () => {
      try {
        await refreshCsrfToken();
        const currentUser = await requestApi('/auth/me');
        if (active) setUser(currentUser);
      } catch (error) {
        if (active && error.status !== 401) setAuthError(error.message);
      } finally {
        if (active) setAuthLoading(false);
      }
    };
    loadSession();
    return () => { active = false; };
  }, []);

  const handleLogout = async () => {
    setAuthError('');
    try {
      await requestApi('/auth/logout', { method: 'POST' });
      setUser(null);
      setIsGuest(false);
      setActiveView('basket');
      setBasketItems([]);
      setRecentSearches([]);
      setComparisonResult(null);
    } catch (error) {
      setAuthError(error.message);
    }
  };

  useEffect(() => {
    let active = true;
    if (!user) {
      return () => { active = false; };
    }

    requestApi('/history/searches')
      .then((searches) => {
        if (active) setRecentSearches(searches);
      })
      .catch((error) => {
        if (active) setComparisonError(error.message);
      });

    return () => { active = false; };
  }, [user]);

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
    setComparisonError('');
    setComparisonResult(null);

    try {
      const items = basketItems.map(item => ({
          name: item.productName,
          quantity: Number(item.quantity),
          unit: item.unit
      }));
      if (user) {
        const savedComparison = await requestApi('/history', {
          method: 'POST',
          body: JSON.stringify({ items })
        });
        setComparisonResult(savedComparison.results);
        setRecentSearches((previousSearches) => {
          const uniqueSearches = new Map();
          [...items, ...previousSearches].forEach((item) => {
            const key = item.name.trim().toLowerCase();
            if (key && !uniqueSearches.has(key)) uniqueSearches.set(key, item);
          });
          return [...uniqueSearches.values()].slice(0, 5);
        });
      } else {
        const data = await callGraphql(COMPARE_BASKET_QUERY, { items });
        setComparisonResult(data.compareBasket);
      }
    } catch (error) {
      setComparisonError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const viewHistoryEntry = (entry) => {
    setBasketItems(entry.items.map((item) => ({
      productName: item.name,
      quantity: item.quantity,
      unit: item.unit
    })));
    setComparisonResult(entry.results);
    setComparisonError('');
    setActiveView('basket');
  };

  const hasItems = basketItems.length > 0;

  if (authLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 text-sm text-gray-500">
        Loading your account…
      </div>
    );
  }

  return (
    <div className="bg-gray-50 font-sans min-h-screen text-gray-800 flex flex-col w-full">
      <Header
        user={user}
        activeView={activeView}
        onShowBasket={() => setActiveView('basket')}
        onShowHistory={() => setActiveView('history')}
        onShowUsers={() => setActiveView('users')}
        onShowProfile={() => setActiveView('profile')}
        onLogout={handleLogout}
        onSignIn={() => setIsGuest(false)}
      />
      {authError && (
        <p role="alert" className="mx-auto mt-4 w-full max-w-3xl rounded-lg bg-red-50 p-3 text-sm text-red-700">
          {authError}
        </p>
      )}
      {!user && !isGuest ? (
        <AuthPage
          onLogin={(signedInUser) => { setUser(signedInUser); setIsGuest(false); setAuthError(''); }}
          onContinueAsGuest={() => { setIsGuest(true); setAuthError(''); }}
        />
      ) : activeView === 'users' && user.role === 'SUPER_ADMIN' ? (
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <UserManagement currentUserId={user.id} />
        </main>
      ) : activeView === 'profile' && user ? (
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <UserProfile user={user} onProfileUpdated={setUser} />
        </main>
      ) : activeView === 'history' && user ? (
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <ComparisonHistory onViewComparison={viewHistoryEntry} />
        </main>
      ) : (
        <main className="flex-grow w-full px-4 sm:px-6 lg:px-8 py-4 space-y-4 text-left">
          {comparisonError && (
            <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
              {comparisonError}
            </p>
          )}
          <section className="w-full">
            <AddProductForm onAddItem={handleAddItem} recentSearches={recentSearches} />
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
      )}
    </div>
  );
}