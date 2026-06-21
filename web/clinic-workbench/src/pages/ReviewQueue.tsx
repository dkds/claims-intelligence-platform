import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useClaims } from '../hooks/useClaims'
import { StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { ErrorMessage } from '../components/ErrorMessage'

export function ReviewQueue() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: claims, isPending, error } = useClaims(user!.clinicId, 'PENDING_REVIEW')

  if (isPending) return <Spinner />
  if (error) return <ErrorMessage message="Failed to load review queue." />

  const count = claims?.length ?? 0

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-slate-800">Review Queue</h1>
        <p className="mt-1 text-sm text-slate-500">
          {count === 0
            ? 'No claims awaiting review.'
            : `${count} claim${count !== 1 ? 's' : ''} awaiting adjuster review.`}
        </p>
      </div>

      {count > 0 && (
        <div className="overflow-hidden rounded-xl border border-amber-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-amber-100 bg-amber-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-amber-700">Claim ID</th>
                <th className="px-4 py-3 text-left font-medium text-amber-700">Origin</th>
                <th className="px-4 py-3 text-left font-medium text-amber-700">Pet</th>
                <th className="px-4 py-3 text-left font-medium text-amber-700">Fraud Risk</th>
                <th className="px-4 py-3 text-right font-medium text-amber-700">Total</th>
                <th className="px-4 py-3 text-left font-medium text-amber-700">Assembled</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {claims!.map(c => (
                <tr
                  key={c._id}
                  onClick={() => navigate(`/claims/${c._id}`)}
                  className="cursor-pointer hover:bg-amber-50 transition-colors"
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
                    {c.fraud ? (
                      <StatusBadge status={c.fraud.riskLevel} />
                    ) : (
                      <span className="text-slate-400 text-xs">—</span>
                    )}
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
      )}
    </div>
  )
}
