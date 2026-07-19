import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useSessions } from '../hooks/useSessions';
import { StatusBadge } from '../components/StatusBadge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import type { Session } from '../api/sessions';

type Filter = 'all' | 'LOGGED' | 'VERIFIED';

export function SessionsList() {
  const { user } = useAuth();
  const { data: sessions, isPending, error } = useSessions(user!.clinicId);
  const navigate = useNavigate();
  const [filter, setFilter] = useState<Filter>('all');

  if (isPending) return <Spinner />;
  if (error) return <ErrorMessage message="Failed to load sessions." />;

  const filtered: Session[] =
    filter === 'all'
      ? (sessions ?? [])
      : (sessions ?? []).filter((s) => s.status === filter);

  const filters: { key: Filter; label: string }[] = [
    { key: 'all', label: 'All' },
    { key: 'LOGGED', label: 'Pending' },
    { key: 'VERIFIED', label: 'Verified' },
  ];

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-slate-800">Sessions</h1>

      <div className="mb-4 flex gap-2">
        {filters.map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              filter === f.key
                ? 'bg-indigo-600 text-white'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Session ID
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Pet ID
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Vet ID
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Logged
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {filtered.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-8 text-center text-slate-400"
                >
                  No sessions found.
                </td>
              </tr>
            )}
            {filtered.map((s) => (
              <tr
                key={s._id}
                onClick={() => navigate(`/sessions/${s._id}`)}
                className="cursor-pointer hover:bg-slate-50 transition-colors"
              >
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {s._id.slice(0, 8)}…
                </td>
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {s.petId.slice(0, 8)}…
                </td>
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {s.vetId.slice(0, 8)}…
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={s.status} />
                </td>
                <td className="px-4 py-3 text-slate-500">
                  {new Date(s.loggedAt).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
