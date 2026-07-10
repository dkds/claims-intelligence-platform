import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const NAV_LINK_BASE =
  'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-slate-300 hover:bg-slate-700 hover:text-white transition-colors'
const NAV_LINK_ACTIVE = 'bg-slate-700 text-white'

function NavItem({ to, label }: { to: string; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        isActive ? `${NAV_LINK_BASE} ${NAV_LINK_ACTIVE}` : NAV_LINK_BASE
      }
    >
      {label}
    </NavLink>
  )
}

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="flex w-56 flex-col bg-slate-800">
        <div className="px-4 py-5">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Claims Intelligence
          </p>
        </div>

        <nav className="flex-1 space-y-1 px-2 py-2">
          <NavItem to="/" label="Dashboard" />
          {user?.role === 'clinic_manager' && (
            <>
              <NavItem to="/sessions" label="Sessions" />
              <NavItem to="/clinics" label="Clinics" />
              <NavItem to="/vets" label="Vets" />
            </>
          )}
          <NavItem to="/claims" label="Claims" />
          {user?.role === 'adjuster' && (
            <NavItem to="/review" label="Review Queue" />
          )}
        </nav>

        <div className="border-t border-slate-700 px-4 py-4">
          <p className="text-sm font-medium text-slate-200">{user?.name}</p>
          <p className="text-xs text-slate-400 capitalize">{user?.role.replace('_', ' ')}</p>
          <button
            onClick={handleLogout}
            className="mt-3 text-xs text-slate-400 hover:text-slate-200 transition-colors"
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
