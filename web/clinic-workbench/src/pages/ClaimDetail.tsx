import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useClaim, useApproveClaim, useRejectClaim } from '../hooks/useClaims'
import { StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { ErrorMessage } from '../components/ErrorMessage'

export function ClaimDetail() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: claim, isPending, error } = useClaim(id!)
  const [showRejectReason, setShowRejectReason] = useState(false)
  const [rejectReason, setRejectReason] = useState('')

  const approve = useApproveClaim(id!, claim?.clinicId ?? '')
  const reject = useRejectClaim(id!, claim?.clinicId ?? '')

  if (isPending) return <Spinner />
  if (error || !claim) return <ErrorMessage message="Claim not found." />

  function handleApprove() {
    approve.mutate(user!.id)
  }

  function handleReject() {
    reject.mutate({ rejectedBy: user!.id, reason: rejectReason || undefined })
  }

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

      {user?.role === 'adjuster' && claim.status === 'PENDING_REVIEW' && (
        <div className="mb-6 rounded-xl border border-amber-200 bg-amber-50 p-4">
          <p className="mb-3 text-sm text-amber-700">
            This claim is awaiting your decision. Review the details above before approving or
            rejecting.
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={handleApprove}
              disabled={approve.isPending || reject.isPending}
              className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50 transition-colors"
            >
              {approve.isPending ? 'Approving…' : 'Approve'}
            </button>
            {!showRejectReason ? (
              <button
                onClick={() => setShowRejectReason(true)}
                disabled={approve.isPending || reject.isPending}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            ) : (
              <button
                onClick={() => setShowRejectReason(false)}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
              >
                Cancel
              </button>
            )}
          </div>
          {showRejectReason && (
            <div className="mt-3">
              <textarea
                value={rejectReason}
                onChange={e => setRejectReason(e.target.value)}
                placeholder="Reason for rejection (optional)"
                rows={3}
                className="w-full rounded-md border border-slate-300 p-2 text-sm"
              />
              <button
                onClick={handleReject}
                disabled={reject.isPending}
                className="mt-2 rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
              >
                {reject.isPending ? 'Rejecting…' : 'Confirm reject'}
              </button>
            </div>
          )}
          {(approve.isError || reject.isError) && (
            <p className="mt-2 text-xs text-red-600">Action failed. Please try again.</p>
          )}
        </div>
      )}

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
