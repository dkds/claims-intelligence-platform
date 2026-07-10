import { useState, type SubmitEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useCreateVet } from '../hooks/useEnrollment'

export function NewVet() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const submit = useCreateVet(user!.clinicId)

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [email, setEmail] = useState('')

  function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    submit.mutate(
      {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        licenseNumber: licenseNumber.trim(),
        email: email.trim() || undefined,
      },
      { onSuccess: () => navigate('/vets') },
    )
  }

  const inputCls =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'
  const labelCls = 'mb-1 block text-sm font-medium text-slate-700'

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => navigate('/vets')}
        className="mb-4 text-sm text-indigo-600 hover:underline"
      >
        ← Back to vets
      </button>
      <h1 className="mb-6 text-2xl font-semibold text-slate-800">Register Vet</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelCls}>First Name</label>
              <input
                required
                value={firstName}
                onChange={e => setFirstName(e.target.value)}
                className={inputCls}
              />
            </div>
            <div>
              <label className={labelCls}>Last Name</label>
              <input
                required
                value={lastName}
                onChange={e => setLastName(e.target.value)}
                className={inputCls}
              />
            </div>
          </div>
          <div>
            <label className={labelCls}>License Number</label>
            <input
              required
              value={licenseNumber}
              onChange={e => setLicenseNumber(e.target.value)}
              className={inputCls}
            />
          </div>
          <div>
            <label className={labelCls}>Email</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={inputCls}
            />
          </div>
        </div>

        {submit.isError && (
          <p className="text-sm text-red-600">Registration failed. Please try again.</p>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={submit.isPending}
            className="rounded-md bg-indigo-600 px-6 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {submit.isPending ? 'Registering…' : 'Register Vet'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/vets')}
            className="rounded-md border border-slate-200 px-6 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
