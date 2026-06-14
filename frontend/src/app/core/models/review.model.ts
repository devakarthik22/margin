import { Finding } from './finding.model';
import { ProviderType, Severity, Verdict } from './review.enums';

export interface ReviewRequest {
  owner: string;
  repo: string;
  number: number;
  headSha: string;
  provider?: ProviderType;
}

export interface ReviewResponse {
  verdict: Verdict;
  summary: string;
  findings: Finding[];
  countsBySeverity: Partial<Record<Severity, number>>;
}
