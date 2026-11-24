export default function Header({ user, activeView, onShowBasket, onShowHistory, onShowUsers, onShowProfile, onLogout, onSignIn }) {
  return (
    <header className="bg-indigo-600 text-white shadow-md w-full">
      <div className="w-full px-4 sm:px-6 lg:px-8 py-2.5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center space-x-2">
          <span className="text-xl sm:text-2xl">🛒</span>
          <div>
            <h5 className="text-sm sm:text-base font-bold tracking-tight leading-none">TrolleyCompare 1234567890</h5>
            <p className="text-[10px] sm:text-[11px] text-indigo-200 mt-0.5">Irish Grocery Price Comparison Engine</p>
          </div>
        </div>
        {user ? (
          <div className="flex flex-wrap items-center gap-3 text-sm">
            {onShowBasket && (
              <button onClick={onShowBasket} className={`rounded-md px-2 py-1 hover:bg-indigo-500 ${activeView === 'basket' ? 'bg-indigo-500' : ''}`}>
                Compare prices
              </button>
            )}
            <button onClick={onShowHistory} className={`rounded-md px-2 py-1 hover:bg-indigo-500 ${activeView === 'history' ? 'bg-indigo-500' : ''}`}>
              History
            </button>
            {user.role === 'SUPER_ADMIN' && (
              <button onClick={onShowUsers} className={`rounded-md px-2 py-1 hover:bg-indigo-500 ${activeView === 'users' ? 'bg-indigo-500' : ''}`}>
                Manage users
              </button>
            )}
            <button onClick={onShowProfile} className={`rounded-md px-2 py-1 hover:bg-indigo-500 ${activeView === 'profile' ? 'bg-indigo-500' : ''}`}>
              My account
            </button>
            <span className="hidden text-indigo-100 sm:inline">{user.name}</span>
            <button onClick={onLogout} className="rounded-md border border-indigo-300 px-3 py-1.5 font-medium hover:bg-indigo-500">
              Sign out
            </button>
          </div>
        ) : (
          <button onClick={onSignIn} className="rounded-md border border-indigo-300 px-3 py-1.5 text-sm font-medium hover:bg-indigo-500">
            Sign in
          </button>
        )}
      </div>
    </header>
  );
}