import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { SessionsList } from './pages/SessionsList';
import { SessionDetail } from './pages/SessionDetail';
import { ClaimsList } from './pages/ClaimsList';
import { ClaimDetail } from './pages/ClaimDetail';
import { NewClaim } from './pages/NewClaim';
import { ReviewQueue } from './pages/ReviewQueue';
import { ClinicsList } from './pages/ClinicsList';
import { NewClinic } from './pages/NewClinic';
import { VetsList } from './pages/VetsList';
import { NewVet } from './pages/NewVet';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          { path: '/', element: <Dashboard /> },
          {
            element: <ProtectedRoute requiredRole="clinic_manager" />,
            children: [
              { path: '/sessions', element: <SessionsList /> },
              { path: '/sessions/:id', element: <SessionDetail /> },
              { path: '/claims/new', element: <NewClaim /> },
              { path: '/clinics', element: <ClinicsList /> },
              { path: '/clinics/new', element: <NewClinic /> },
              { path: '/vets', element: <VetsList /> },
              { path: '/vets/new', element: <NewVet /> },
            ],
          },
          { path: '/claims', element: <ClaimsList /> },
          { path: '/claims/:id', element: <ClaimDetail /> },
          {
            element: <ProtectedRoute requiredRole="adjuster" />,
            children: [{ path: '/review', element: <ReviewQueue /> }],
          },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
