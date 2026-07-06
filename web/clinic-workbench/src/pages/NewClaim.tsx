import { useState, type SubmitEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useSubmitClaim } from '../hooks/useClaims'

interface Line {
  procedureCode: string
  quantity: string
  requestedAmount: string
}

const emptyLine = (): Line => ({ procedureCode: '', quantity: '1', requestedAmount: '' })

export function NewClaim() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const submit = useSubmitClaim(user!.clinicId)

  const [petId, setPetId] = useState('')
  const [policyId, setPolicyId] = useState('')
  const [lines, setLines] = useState<Line[]>([emptyLine()])

  function updateLine(i: number, field: keyof Line, value: string) {
    setLines(prev => prev.map((l, idx) => (idx === i ? { ...l, [field]: value } : l)))
  }

  function addLine() {
    setLines(prev => [...prev, emptyLine()])
  }

  function removeLine(i: number) {
    setLines(prev => prev.filter((_, idx) => idx !== i))
  }

  function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    submit.mutate(
      {
        petId: petId.trim(),
        policyId: policyId.trim(),
        submittedBy: user!.id,
        lines: lines.map(l => ({
          procedureCode: l.procedureCode.trim(),
          quantity: parseInt(l.quantity, 10),
          requestedAmount: parseFloat(l.requestedAmount),
        })),
      },
      { onSuccess: () => navigate('/claims') },
    )
  }

  const inputCls =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'
  const labelCls = 'mb-1 block text-sm font-medium text-slate-700'

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => navigate('/claims')}
        className="mb-4 text-sm text-indigo-600 hover:underline"
      >
        ← Back to claims
      </button>
      <h1 className="mb-6 text-2xl font-semibold text-slate-800">Submit Manual Claim</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
          <div>
            <label className={labelCls}>Pet ID</label>
            <input
              required
              value={petId}
              onChange={e => setPetId(e.target.value)}
              className={inputCls}
              placeholder="UUID"
            />
          </div>
          <div>
            <label className={labelCls}>Policy ID</label>
            <input
              required
              value={policyId}
              onChange={e => setPolicyId(e.target.value)}
              className={inputCls}
              placeholder="UUID"
            />
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-700">Claim Lines</h2>
          <div className="space-y-3">
            {lines.map((line, i) => (
              <div key={i} className="flex gap-3 items-end">
                <div className="flex-1">
                  {i === 0 && <label className={labelCls}>Procedure Code</label>}
                  <input
                    required
                    value={line.procedureCode}
                    onChange={e => updateLine(i, 'procedureCode', e.target.value)}
                    className={inputCls}
                    placeholder="e.g. DENT-CLEAN"
                  />
                </div>
                <div className="w-20">
                  {i === 0 && <label className={labelCls}>Qty</label>}
                  <input
                    required
                    type="number"
                    min={1}
                    value={line.quantity}
                    onChange={e => updateLine(i, 'quantity', e.target.value)}
                    className={inputCls}
                  />
                </div>
                <div className="w-28">
                  {i === 0 && <label className={labelCls}>Amount (£)</label>}
                  <input
                    required
                    type="number"
                    min={0.01}
                    step={0.01}
                    value={line.requestedAmount}
                    onChange={e => updateLine(i, 'requestedAmount', e.target.value)}
                    className={inputCls}
                    placeholder="0.00"
                  />
                </div>
                {lines.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeLine(i)}
                    className="mb-0 text-slate-400 hover:text-red-500 transition-colors text-lg leading-none"
                    aria-label="Remove line"
                  >
                    ×
                  </button>
                )}
              </div>
            ))}
          </div>
          <button
            type="button"
            onClick={addLine}
            className="mt-3 text-sm text-indigo-600 hover:underline"
          >
            + Add line
          </button>
        </div>

        {submit.isError && (
          <p className="text-sm text-red-600">Submission failed. Check IDs and try again.</p>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={submit.isPending}
            className="rounded-md bg-indigo-600 px-6 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {submit.isPending ? 'Submitting…' : 'Submit Claim'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/claims')}
            className="rounded-md border border-slate-200 px-6 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
