import { Category, Severity } from './review.enums';

export interface Finding {
  category: Category;
  severity: Severity;
  line: number | null;
  filePath: string | null;
  title: string;
  explanation: string;
  suggestion: string | null;
}
