import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useVets, useApproveVet, useRejectVet } from '../hooks/useEnrollment';
import { StatusBadge } from '../components/StatusBadge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import type { Vet } from '../api/enrollment';

function VetRow({ vet, clinicId }: { vet: Vet; clinicId: string }) {
  const [showRejectReason, setShowRejectReason] = useState(false);
  const [reason, setReason] = useState('');
  const approve = useApproveVet(vet._id, clinicId);
  const reject = useRejectVet(vet._id, clinicId);

  return (
    <tr>
      <td className="px-4 py-3 text-slate-700">
        {vet.firstName} {vet.lastName}
      </td>
      <td className="px-4 py-3">
        <StatusBadge status={vet.status} />
      </td>
      <td className="px-4 py-3 text-slate-500">{vet.rejectionReason || '—'}</td>
      <td className="px-4 py-3">
        {vet.status === 'PENDING' && (
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => approve.mutate()}
                disabled={approve.isPending || reject.isPending}
                className="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50 transition-colors"
              >
                {approve.isPending ? 'Approving…' : 'Approve'}
              </button>
              {!showRejectReason ? (
                <button
                  onClick={() => setShowRejectReason(true)}
                  disabled={approve.isPending || reject.isPending}
                  className="rounded-md bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
                >
                  Reject
                </button>
              ) : (
                <button
                  onClick={() => setShowRejectReason(false)}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
              )}
            </div>
            {showRejectReason && (
              <div className="mt-2 flex items-center gap-2">
                <input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Reason for rejection"
                  className="w-48 rounded-md border border-slate-300 px-2 py-1 text-xs"
                />
                <button
                  onClick={() => reject.mutate(reason)}
                  disabled={reject.isPending || !reason.trim()}
                  className="rounded-md bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
                >
                  {reject.isPending ? 'Rejecting…' : 'Confirm'}
                </button>
              </div>
            )}
            {(approve.isError || reject.isError) && (
              <p className="mt-1 text-xs text-red-600">
                Action failed. Please try again.
              </p>
            )}
          </div>
        )}
      </td>
    </tr>
  );
}

export function VetsList() {
  const { user } = useAuth();
  const { data: vets, isPending, error } = useVets(user!.clinicId);

  if (isPending) return <Spinner />;
  if (error) return <ErrorMessage message="Failed to load vets." />;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800">Vets</h1>
        <Link
          to="/vets/new"
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
        >
          + Register Vet
        </Link>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Name
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Rejection Reason
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {(vets ?? []).length === 0 && (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-slate-400"
                >
                  No vets found.
                </td>
              </tr>
            )}
            {(vets ?? []).map((v) => (
              <VetRow key={v._id} vet={v} clinicId={user!.clinicId} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
