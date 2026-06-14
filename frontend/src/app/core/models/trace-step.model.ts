export type TraceKind = 'sys' | 'tool' | 'ret' | 'done';

export interface TraceStep {
  kind: TraceKind;
  text: string;
}
