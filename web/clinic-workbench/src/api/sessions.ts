import client from './client';

export interface SessionLine {
  lineId: string;
  description: string;
  amount: number;
  category: string;
}

export interface Session {
  _id: string;
  petId: string;
  vetId: string;
  clinicId: string;
  status: string;
  lines: SessionLine[];
  loggedAt: string;
  verifiedBy?: string;
  verifiedAt?: string;
}

export const listSessions = (clinicId: string) =>
  client.get<Session[]>(`/clinics/${clinicId}/sessions`).then((r) => r.data);

export const getSession = (id: string) =>
  client.get<Session>(`/sessions/${id}`).then((r) => r.data);

export const verifySession = (id: string, verifiedBy: string) =>
  client.post(`/sessions/${id}/verify`, { verifiedBy }).then((r) => r.data);
