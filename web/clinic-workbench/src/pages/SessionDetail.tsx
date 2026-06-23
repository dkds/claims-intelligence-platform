import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useSession, useVerifySession } from '../hooks/useSessions'
import { StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { ErrorMessage } from '../components/ErrorMessage'

export function SessionDetail() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { data: session, isPending, error } = useSession(id!)
  const verify = useVerifySession(id!, user!.clinicId)

  if (isPending) return <Spinner />
  if (error || !session) return <ErrorMessage message="Session not found." />

  function handleVerify() {
    verify.mutate(user!.id, { onSuccess: () => navigate('/sessions') })
  }

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => navigate('/sessions')}
        className="mb-4 text-sm text-indigo-600 hover:underline"
      >
        ← Back to sessions
      </button>

      <div className="mb-6 flex items-center gap-3">
        <h1 className="text-2xl font-semibold text-slate-800">Session</h1>
        <StatusBadge status={session.status} />
      </div>

      <div className="mb-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <dl className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-slate-500">Session ID</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{session._id}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Clinic</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{session.clinicId}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Pet</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{session.petId}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Vet</dt>
            <dd className="mt-1 font-mono text-xs text-slate-700">{session.vetId}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Logged</dt>
            <dd className="mt-1 text-slate-700">{new Date(session.loggedAt).toLocaleString()}</dd>
          </div>
          {session.verifiedAt && (
            <div>
              <dt className="font-medium text-slate-500">Verified</dt>
              <dd className="mt-1 text-slate-700">
                {new Date(session.verifiedAt).toLocaleString()} by {session.verifiedBy}
              </dd>
            </div>
          )}
        </dl>
      </div>

      {session.lines.length > 0 && (
        <div className="mb-6 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <h2 className="border-b border-slate-200 px-6 py-3 text-sm font-semibold text-slate-700">
            Treatment Lines
          </h2>
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-2 text-left font-medium text-slate-500">Description</th>
                <th className="px-4 py-2 text-left font-medium text-slate-500">Category</th>
                <th className="px-4 py-2 text-right font-medium text-slate-500">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {session.lines.map(l => (
                <tr key={l.lineId}>
                  <td className="px-4 py-2 text-slate-700">{l.description}</td>
                  <td className="px-4 py-2 text-slate-500">{l.category}</td>
                  <td className="px-4 py-2 text-right text-slate-700">
                    £{l.amount.toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {session.status === 'LOGGED' && (
        <div className="rounded-xl border border-teal-200 bg-teal-50 p-4">
          <p className="mb-3 text-sm text-teal-700">
            This session is awaiting verification. Review the treatment lines above before
            confirming.
          </p>
          <button
            onClick={handleVerify}
            disabled={verify.isPending}
            className="rounded-md bg-teal-600 px-4 py-2 text-sm font-medium text-white hover:bg-teal-700 disabled:opacity-50 transition-colors"
          >
            {verify.isPending ? 'Verifying…' : 'Verify Session'}
          </button>
          {verify.isError && (
            <p className="mt-2 text-xs text-red-600">Verification failed. Please try again.</p>
          )}
        </div>
      )}
    </div>
  )
}
