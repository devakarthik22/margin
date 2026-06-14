import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/review/review-page/review-page.component')
        .then((m) => m.ReviewPageComponent),
  },
];
