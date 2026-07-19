import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useSessions } from '../hooks/useSessions';
import { useClaims } from '../hooks/useClaims';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';

function StatCard({
  label,
  value,
  to,
  highlight,
}: {
  label: string;
  value: number | string;
  to: string;
  highlight?: boolean;
}) {
  return (
    <Link
      to={to}
      className={`block rounded-xl border p-6 shadow-sm transition-shadow hover:shadow-md ${
        highlight ? 'border-amber-200 bg-amber-50' : 'border-slate-200 bg-white'
      }`}
    >
      <p className="text-3xl font-bold text-slate-800">{value}</p>
      <p className="mt-1 text-sm text-slate-500">{label}</p>
    </Link>
  );
}

export function Dashboard() {
  const { user } = useAuth();
  const {
    data: sessions,
    isPending: sessionsPending,
    error: sessionsError,
  } = useSessions(user!.clinicId);
  const {
    data: claims,
    isPending: claimsPending,
    error: claimsError,
  } = useClaims(user!.clinicId);

  if (sessionsPending || claimsPending) return <Spinner />;
  if (sessionsError || claimsError)
    return <ErrorMessage message="Failed to load dashboard." />;

  const unverified = sessions?.filter((s) => s.status === 'LOGGED').length ?? 0;
  const pendingReview =
    claims?.filter((c) => c.status === 'PENDING_REVIEW').length ?? 0;
  const adjudicated =
    claims?.filter((c) => c.status === 'ADJUDICATED').length ?? 0;

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-slate-800">Dashboard</h1>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {user?.role === 'clinic_manager' && (
          <>
            <StatCard
              label="Total sessions"
              value={sessions?.length ?? 0}
              to="/sessions"
            />
            <StatCard
              label="Awaiting verification"
              value={unverified}
              to="/sessions"
              highlight={unverified > 0}
            />
          </>
        )}
        <StatCard
          label="Total claims"
          value={claims?.length ?? 0}
          to="/claims"
        />
        <StatCard
          label="Pending review"
          value={pendingReview}
          to="/review"
          highlight={pendingReview > 0}
        />
        <StatCard label="Adjudicated" value={adjudicated} to="/claims" />
      </div>
    </div>
  );
}
