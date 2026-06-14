/** Parsed PR contents for the file-browser view (mirrors backend PrFilesResponse). */
export interface PrFiles {
  owner: string;
  repo: string;
  number: number;
  title: string | null;
  files: PrFile[];
}

export interface PrFile {
  path: string;
  additions: number;
  deletions: number;
  lines: PrLine[];
}

/** type: 'add' | 'del' | 'ctx' | 'hunk' */
export interface PrLine {
  type: 'add' | 'del' | 'ctx' | 'hunk';
  line: number | null;
  content: string;
}
