import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useClaims } from '../hooks/useClaims'
import { StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { ErrorMessage } from '../components/ErrorMessage'

const STATUSES = [
  { key: undefined, label: 'All' },
  { key: 'ASSEMBLED', label: 'Assembled' },
  { key: 'PENDING_REVIEW', label: 'Pending Review' },
  { key: 'ADJUDICATED', label: 'Adjudicated' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'READY_FOR_SUBMISSION', label: 'Ready' },
] as const

export function ClaimsList() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [status, setStatus] = useState<string | undefined>(undefined)
  const { data: claims, isPending, error } = useClaims(user!.clinicId, status)

  if (isPending) return <Spinner />
  if (error) return <ErrorMessage message="Failed to load claims." />

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800">Claims</h1>
        {user?.role === 'clinic_manager' && (
          <Link
            to="/claims/new"
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
          >
            + New Claim
          </Link>
        )}
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        {STATUSES.map(s => (
          <button
            key={s.label}
            onClick={() => setStatus(s.key)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              status === s.key
                ? 'bg-indigo-600 text-white'
                : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-slate-500">Claim ID</th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">Origin</th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">Pet</th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">Status</th>
              <th className="px-4 py-3 text-right font-medium text-slate-500">Total</th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">Assembled</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {(claims ?? []).length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-400">
                  No claims found.
                </td>
              </tr>
            )}
            {(claims ?? []).map(c => (
              <tr
                key={c._id}
                onClick={() => navigate(`/claims/${c._id}`)}
                className="cursor-pointer hover:bg-slate-50 transition-colors"
              >
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {c._id.slice(0, 8)}…
                </td>
                <td className="px-4 py-3">
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 uppercase">
                    {c.origin}
                  </span>
                </td>
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  {c.petId.slice(0, 8)}…
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={c.status} />
                </td>
                <td className="px-4 py-3 text-right font-medium text-slate-700">
                  £{c.totalRequested.toFixed(2)}
                </td>
                <td className="px-4 py-3 text-slate-500">
                  {new Date(c.assembledAt).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
