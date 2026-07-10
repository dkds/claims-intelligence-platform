import { useState, type SubmitEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCreateClinic } from '../hooks/useEnrollment'

export function NewClinic() {
  const navigate = useNavigate()
  const submit = useCreateClinic()

  const [name, setName] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [city, setCity] = useState('')
  const [postcode, setPostcode] = useState('')

  function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    submit.mutate(
      {
        name: name.trim(),
        contactEmail: contactEmail.trim() || undefined,
        city: city.trim() || undefined,
        postcode: postcode.trim() || undefined,
      },
      { onSuccess: () => navigate('/clinics') },
    )
  }

  const inputCls =
    'w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'
  const labelCls = 'mb-1 block text-sm font-medium text-slate-700'

  return (
    <div className="max-w-2xl">
      <button
        onClick={() => navigate('/clinics')}
        className="mb-4 text-sm text-indigo-600 hover:underline"
      >
        ← Back to clinics
      </button>
      <h1 className="mb-6 text-2xl font-semibold text-slate-800">Register Clinic</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
          <div>
            <label className={labelCls}>Name</label>
            <input
              required
              value={name}
              onChange={e => setName(e.target.value)}
              className={inputCls}
            />
          </div>
          <div>
            <label className={labelCls}>Contact Email</label>
            <input
              type="email"
              value={contactEmail}
              onChange={e => setContactEmail(e.target.value)}
              className={inputCls}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelCls}>City</label>
              <input value={city} onChange={e => setCity(e.target.value)} className={inputCls} />
            </div>
            <div>
              <label className={labelCls}>Postcode</label>
              <input
                value={postcode}
                onChange={e => setPostcode(e.target.value)}
                className={inputCls}
              />
            </div>
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
            {submit.isPending ? 'Registering…' : 'Register Clinic'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/clinics')}
            className="rounded-md border border-slate-200 px-6 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
