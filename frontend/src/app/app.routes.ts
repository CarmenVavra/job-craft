import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { ShellComponent } from './shared/layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Anmelden – JobCraft',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'uebersicht' },
      {
        path: 'uebersicht',
        title: 'Übersicht – JobCraft',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'bewerbung/neu',
        title: 'Neue Bewerbung – JobCraft',
        loadComponent: () =>
          import('./features/new-application/new-application.component').then(
            (m) => m.NewApplicationComponent,
          ),
      },
      {
        path: 'bewerbung/neu/:id',
        title: 'Neue Bewerbung – JobCraft',
        loadComponent: () =>
          import('./features/new-application/new-application.component').then(
            (m) => m.NewApplicationComponent,
          ),
      },
      {
        path: 'bewerbung/:id',
        title: 'Anschreiben – JobCraft',
        loadComponent: () =>
          import('./features/editor/editor.component').then((m) => m.EditorComponent),
      },
      {
        path: 'dokumente',
        title: 'Dokumente – JobCraft',
        loadComponent: () =>
          import('./features/documents/documents.component').then((m) => m.DocumentsComponent),
      },
      {
        path: 'briefkopf',
        title: 'Briefkopf – JobCraft',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
