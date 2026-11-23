import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent)
  },
  {
    path: 'catalog',
    loadComponent: () =>
      import('./features/catalog/catalog-page.component').then((m) => m.CatalogPageComponent)
  },
  {
    path: 'decks',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/deck-builder/deck-list-page.component').then((m) => m.DeckListPageComponent)
  },
  {
    path: 'decks/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/deck-builder/deck-editor-page.component').then((m) => m.DeckEditorPageComponent)
  },
  {
    path: 'decks/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/deck-builder/deck-editor-page.component').then((m) => m.DeckEditorPageComponent)
  },
  {
    path: 'play/setup',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/play/match-setup-page.component').then((m) => m.MatchSetupPageComponent)
  },
  {
    path: 'play/:matchId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/play/battle-board-page.component').then((m) => m.BattleBoardPageComponent)
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login-page.component').then((m) => m.LoginPageComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register-page.component').then((m) => m.RegisterPageComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
