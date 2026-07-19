import { Link } from 'react-router-dom';
import { useClinics } from '../hooks/useEnrollment';
import { StatusBadge } from '../components/StatusBadge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';

export function ClinicsList() {
  const { data: clinics, isPending, error } = useClinics();

  if (isPending) return <Spinner />;
  if (error) return <ErrorMessage message="Failed to load clinics." />;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-800">Clinics</h1>
        <Link
          to="/clinics/new"
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
        >
          + Register Clinic
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
                Contact Email
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Status
              </th>
              <th className="px-4 py-3 text-left font-medium text-slate-500">
                Updated
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {(clinics ?? []).length === 0 && (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-slate-400"
                >
                  No clinics found.
                </td>
              </tr>
            )}
            {(clinics ?? []).map((c) => (
              <tr key={c._id}>
                <td className="px-4 py-3 text-slate-700">{c.name}</td>
                <td className="px-4 py-3 text-slate-500">
                  {c.contactEmail || '—'}
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={c.status} />
                </td>
                <td className="px-4 py-3 text-slate-500">
                  {c.updatedAt
                    ? new Date(c.updatedAt).toLocaleDateString()
                    : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
