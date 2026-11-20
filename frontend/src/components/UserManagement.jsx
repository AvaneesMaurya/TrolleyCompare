import { useCallback, useEffect, useState } from 'react';
import { requestApi } from '../services/api';

const EMPTY_FORM = { name: '', email: '', password: '', enabled: true, role: 'USER' };

export default function UserManagement({ currentUserId }) {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const loadUsers = useCallback(async () => {
    return requestApi('/admin/users');
  }, []);

  useEffect(() => {
    let active = true;
    const fetchUsers = async () => {
      try {
        const results = await loadUsers();
        if (active) setUsers(results);
      } catch (requestError) {
        if (active) setError(requestError.message);
      }
    };
    void fetchUsers();
    return () => { active = false; };
  }, [loadUsers]);

  const resetForm = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError('');
  };

  const editUser = (user) => {
    setEditingId(user.id);
    setForm({ name: user.name, email: user.email, password: '', enabled: user.enabled, role: user.role });
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const body = editingId
        ? { ...form, password: form.password || null }
        : { name: form.name, email: form.email, password: form.password };
      await requestApi(editingId ? `/admin/users/${encodeURIComponent(editingId)}` : '/admin/users', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify(body)
      });
      resetForm();
      setUsers(await loadUsers());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const deleteUser = async (user) => {
    if (!window.confirm(`Delete the account for ${user.email}? This cannot be undone.`)) return;
    setError('');
    try {
      await requestApi(`/admin/users/${encodeURIComponent(user.id)}`, { method: 'DELETE' });
      setUsers(await loadUsers());
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  return (
    <section className="space-y-5 text-left">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">User management</h1>
        <p className="mt-1 text-sm text-gray-500">Create, update, and remove user accounts.</p>
      </div>

      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}

      <form onSubmit={handleSubmit} className="grid gap-3 rounded-xl border border-gray-100 bg-white p-4 shadow-sm sm:grid-cols-2 lg:grid-cols-6">
        <h2 className="text-base font-semibold text-gray-800 sm:col-span-2 lg:col-span-5">
          {editingId ? 'Edit user' : 'Create user'}
        </h2>
        <label className="text-sm font-medium text-gray-700">
          Name
          <input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        <label className="text-sm font-medium text-gray-700">
          Email
          <input type="email" required maxLength={254} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        <label className="text-sm font-medium text-gray-700">
          Password {editingId && <span className="font-normal text-gray-400">(leave blank to keep)</span>}
          <input type="password" required={!editingId} minLength={8} maxLength={72} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        {editingId && (
          <>
            <label className="text-sm font-medium text-gray-700">
              Role
              <select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })} className="mt-1 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 font-normal">
                <option value="USER">User</option>
                <option value="SUPER_ADMIN">Super admin</option>
              </select>
            </label>
          <label className="flex items-center gap-2 self-end pb-2 text-sm text-gray-700">
            <input type="checkbox" checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} />
            Account enabled
          </label>
          </>
        )}
        <div className="flex items-end gap-2">
          <button type="submit" disabled={busy} className="rounded-lg bg-indigo-600 px-4 py-2 font-semibold text-white hover:bg-indigo-700 disabled:opacity-60">
            {editingId ? 'Save changes' : 'Create user'}
          </button>
          {editingId && <button type="button" onClick={resetForm} className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50">Cancel</button>}
        </div>
      </form>

      <div className="overflow-x-auto rounded-xl border border-gray-100 bg-white shadow-sm">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {users.map((user) => {
              const isSuperAdmin = user.role === 'SUPER_ADMIN';
              const isCurrentUser = user.id === currentUserId;
              return (
                <tr key={user.id}>
                  <td className="px-4 py-3 font-medium text-gray-900">{user.name}</td>
                  <td className="px-4 py-3 text-gray-600">{user.email}</td>
                  <td className="px-4 py-3 text-gray-600">{isSuperAdmin ? 'Super admin' : 'User'}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2 py-1 text-xs font-medium ${user.enabled ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                      {user.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    {isCurrentUser ? <span className="text-xs text-gray-400">Current account protected</span> : (
                      <div className="flex justify-end gap-3">
                        <button type="button" onClick={() => editUser(user)} className="font-medium text-indigo-600 hover:text-indigo-800">Edit</button>
                        {!isSuperAdmin && <button type="button" onClick={() => deleteUser(user)} className="font-medium text-red-600 hover:text-red-800">Delete</button>}
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
            {users.length === 0 && (
              <tr><td colSpan="5" className="px-4 py-8 text-center text-gray-500">No user accounts found.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
