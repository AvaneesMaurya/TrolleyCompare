import { useState } from 'react';
import { requestApi } from '../services/api';

export default function AuthPage({ onLogin, onContinueAsGuest }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');
    setBusy(true);
    try {
      if (mode === 'register') {
        await requestApi('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ name, email, password })
        });
        setMode('login');
        setPassword('');
        setNotice('Account created. Sign in with your new credentials.');
      } else {
        const user = await requestApi('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });
        onLogin(user);
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const switchMode = () => {
    setMode((current) => current === 'login' ? 'register' : 'login');
    setError('');
    setNotice('');
  };

  return (
    <main className="flex flex-1 items-center justify-center bg-gray-50 px-4 py-12 text-left">
      <section className="w-full max-w-md rounded-2xl border border-gray-100 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">
          {mode === 'login' ? 'Welcome back' : 'Create your account'}
        </h1>
        <p className="mb-6 text-sm text-gray-500">
          {mode === 'login'
            ? 'Sign in to compare grocery prices.'
            : 'Register for a TrolleyCompare account.'}
        </p>

        {error && <p role="alert" className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {notice && <p role="status" className="mb-4 rounded-lg bg-green-50 p-3 text-sm text-green-700">{notice}</p>}

        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'register' && (
            <label className="block text-sm font-medium text-gray-700">
              Name
              <input
                autoComplete="name"
                required
                maxLength={100}
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              />
            </label>
          )}
          <label className="block text-sm font-medium text-gray-700">
            Email
            <input
              type="email"
              autoComplete="email"
              required
              maxLength={254}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
            />
          </label>
          <label className="block text-sm font-medium text-gray-700">
            Password
            <input
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
              minLength={8}
              maxLength={72}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
            />
          </label>
          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-wait disabled:opacity-60"
          >
            {busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-600">
          {mode === 'login' ? 'New to TrolleyCompare?' : 'Already have an account?'}{' '}
          <button type="button" onClick={switchMode} className="font-semibold text-indigo-600 hover:text-indigo-700">
            {mode === 'login' ? 'Create an account' : 'Sign in'}
          </button>
        </p>
        {mode === 'login' && (
          <p className="mt-4 text-center">
            <button type="button" onClick={onContinueAsGuest} className="text-sm font-medium text-gray-500 hover:text-gray-800">
              Continue without signing in
            </button>
          </p>
        )}
      </section>
    </main>
  );
}
