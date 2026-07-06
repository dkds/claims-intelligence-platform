import { useParams, useNavigate } from 'react-router-dom'
import { useClaim } from '../hooks/useClaims'
import { StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { ErrorMessage } from '../components/ErrorMessage'

export function ClaimDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: claim, isPending, error } = useClaim(id!)

  if (isPending) return <Spinner />
  if (error || !claim) return <ErrorMessage message="Claim not found." />

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => navigate('/claims')}
        className="mb-4 text-sm text-indigo-600 hover:underline"
      >
        ← Back to claims
      </button>

      <div className="mb-6 flex flex-wrap items-center gap-2">
        <h1 className="text-2xl font-semibold text-slate-800">Claim</h1>
        <StatusBadge status={claim.status} />
        <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 uppercase">
          {claim.origin}
        </span>
      </div>

      <div className="mb-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <dl className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-slate-500">Claim ID</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{claim._id}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Pet</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{claim.petId}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Policy</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{claim.policyId}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Submitted by</dt>
            <dd className="mt-1 text-slate-700">{claim.submittedBy}</dd>
          </div>
          {claim.sourceSessionId && (
            <div>
              <dt className="font-medium text-slate-500">Source session</dt>
              <dd className="mt-1 font-mono text-xs text-slate-700">{claim.sourceSessionId}</dd>
            </div>
          )}
          <div>
            <dt className="font-medium text-slate-500">Total requested</dt>
            <dd className="mt-1 text-lg font-semibold text-slate-800">
              £{claim.totalRequested.toFixed(2)}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Assembled</dt>
            <dd className="mt-1 text-slate-700">{new Date(claim.assembledAt).toLocaleString()}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Last updated</dt>
            <dd className="mt-1 text-slate-700">{new Date(claim.updatedAt).toLocaleString()}</dd>
          </div>
        </dl>
      </div>

      {claim.fraud && (
        <div className="mb-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Fraud Score</h2>
          <div className="flex items-center gap-4">
            <div className="text-center">
              <p className="text-3xl font-bold text-slate-800">
                {(claim.fraud.score * 100).toFixed(0)}%
              </p>
              <p className="text-xs text-slate-500">risk score</p>
            </div>
            <StatusBadge status={claim.fraud.riskLevel} />
            <p className="text-xs text-slate-400">model: {claim.fraud.modelVersion}</p>
          </div>
          {claim.fraud.flags.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1">
              {claim.fraud.flags.map(f => (
                <span
                  key={f}
                  className="rounded bg-red-50 px-2 py-0.5 text-xs font-medium text-red-600"
                >
                  {f}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="mb-6 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <h2 className="border-b border-slate-200 px-6 py-3 text-sm font-semibold text-slate-700">
          Claim Lines
        </h2>
        <table className="w-full text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-4 py-2 text-left font-medium text-slate-500">Procedure</th>
              <th className="px-4 py-2 text-right font-medium text-slate-500">Qty</th>
              <th className="px-4 py-2 text-right font-medium text-slate-500">Amount</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {claim.lines.map((l, i) => (
              <tr key={i}>
                <td className="px-4 py-2 text-slate-700">{l.procedureCode}</td>
                <td className="px-4 py-2 text-right text-slate-500">{l.quantity}</td>
                <td className="px-4 py-2 text-right text-slate-700">£{l.requestedAmount.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {(claim.decision || claim.reasons) && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Decision</h2>
          <dl className="space-y-3 text-sm">
            {claim.decision && (
              <div>
                <dt className="font-medium text-slate-500">Outcome</dt>
                <dd className="mt-1 font-semibold text-slate-800 uppercase">{claim.decision}</dd>
              </div>
            )}
            {claim.approvedAmount !== undefined && (
              <div>
                <dt className="font-medium text-slate-500">Approved amount</dt>
                <dd className="mt-1 text-lg font-semibold text-green-700">
                  £{claim.approvedAmount.toFixed(2)}
                </dd>
              </div>
            )}
            {claim.adjudicatedBy && (
              <div>
                <dt className="font-medium text-slate-500">Adjudicated by</dt>
                <dd className="mt-1 text-slate-700">{claim.adjudicatedBy}</dd>
              </div>
            )}
            {claim.reasons && claim.reasons.length > 0 && (
              <div>
                <dt className="font-medium text-slate-500">Reasons</dt>
                <dd className="mt-1">
                  <ul className="list-disc pl-4 text-slate-700">
                    {claim.reasons.map((r, i) => <li key={i}>{r}</li>)}
                  </ul>
                </dd>
              </div>
            )}
          </dl>
        </div>
      )}
    </div>
  )
}
