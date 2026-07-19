import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listSessions, getSession, verifySession } from '../api/sessions';

export const sessionKeys = {
  list: (clinicId: string) => ['sessions', clinicId] as const,
  detail: (id: string) => ['sessions', 'detail', id] as const,
};

export function useSessions(clinicId: string) {
  return useQuery({
    queryKey: sessionKeys.list(clinicId),
    queryFn: () => listSessions(clinicId),
  });
}

export function useSession(id: string) {
  return useQuery({
    queryKey: sessionKeys.detail(id),
    queryFn: () => getSession(id),
  });
}

export function useVerifySession(sessionId: string, clinicId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verifiedBy: string) => verifySession(sessionId, verifiedBy),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: sessionKeys.list(clinicId) });
      qc.invalidateQueries({ queryKey: sessionKeys.detail(sessionId) });
    },
  });
}
