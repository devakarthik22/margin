// Mirrors the backend enums (lower-cased on the wire).
export type Severity = 'critical' | 'high' | 'medium' | 'low';
export type Category =
  | 'bug' | 'security' | 'performance' | 'convention' | 'maintainability';
export type Verdict = 'approve' | 'comment' | 'request_changes';
export type ProviderType = 'GITHUB' | 'GITLAB' | 'BITBUCKET';

export const SEVERITY_META: Record<Severity, { label: string; color: string }> = {
  critical: { label: 'Critical', color: '#FF5C63' },
  high: { label: 'High', color: '#FF9F45' },
  medium: { label: 'Medium', color: '#FFD24A' },
  low: { label: 'Low', color: '#4EC8A0' },
};

export const VERDICT_META: Record<Verdict, { label: string; color: string }> = {
  approve: { label: 'Approve', color: '#4EC8A0' },
  comment: { label: 'Comment', color: '#7C8CF8' },
  request_changes: { label: 'Request changes', color: '#FF5C63' },
};
