const COLOURS: Record<string, string> = {
  // Session statuses
  LOGGED: 'bg-slate-100 text-slate-700',
  VERIFIED: 'bg-teal-100 text-teal-700',
  // Claim statuses
  ASSEMBLED: 'bg-blue-100 text-blue-700',
  PENDING_REVIEW: 'bg-amber-100 text-amber-700',
  ADJUDICATED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  READY_FOR_SUBMISSION: 'bg-purple-100 text-purple-700',
  // Fraud risk levels
  low: 'bg-green-100 text-green-700',
  medium: 'bg-amber-100 text-amber-700',
  high: 'bg-red-100 text-red-700',
}

export function StatusBadge({ status }: { status: string }) {
  const colour = COLOURS[status] ?? 'bg-gray-100 text-gray-600'
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${colour}`}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}
