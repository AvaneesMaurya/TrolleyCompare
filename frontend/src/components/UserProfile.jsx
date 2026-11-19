import { useState } from 'react';
import { requestApi } from '../services/api';

export default function UserProfile({ user, onProfileUpdated }) {
  const [name, setName] = useState(user.name);
  const [email, setEmail] = useState(user.email);
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
      const updatedUser = await requestApi('/auth/me', {
        method: 'PUT',
        body: JSON.stringify({ name, email, password: password || null })
      });
      onProfileUpdated(updatedUser);
      setName(updatedUser.name);
      setEmail(updatedUser.email);
      setPassword('');
      setNotice('Your profile has been updated.');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-2xl space-y-5 text-left">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">My account</h1>
        <p className="mt-1 text-sm text-gray-500">Update your account details. Your account role cannot be changed here.</p>
      </div>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="rounded-lg bg-green-50 p-3 text-sm text-green-700">{notice}</p>}
      <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
        <label className="block text-sm font-medium text-gray-700">
          Name
          <input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal" />
        </label>
        <label className="block text-sm font-medium text-gray-700">
          Email
          <input type="email" required maxLength={254} value={email} onChange={(event) => setEmail(event.target.value)} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal" />
        </label>
        <label className="block text-sm font-medium text-gray-700">
          New password <span className="font-normal text-gray-400">(leave blank to keep current password)</span>
          <input type="password" autoComplete="new-password" minLength={8} maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal" />
        </label>
        <div className="flex items-center justify-between gap-4">
          <p className="text-sm text-gray-500">Role: <span className="font-semibold text-gray-700">{user.role === 'SUPER_ADMIN' ? 'Super admin' : 'User'}</span></p>
          <button disabled={busy} className="rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white hover:bg-indigo-700 disabled:opacity-60">
            {busy ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
