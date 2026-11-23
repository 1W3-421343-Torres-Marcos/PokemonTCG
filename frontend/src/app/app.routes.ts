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
    loadComponent: () =>
      import('./shared/ui/feature-placeholder/feature-placeholder.component').then(
        (m) => m.FeaturePlaceholderComponent
      ),
    data: {
      phase: 'Fase 3',
      title: 'Combate PvE vs. CPU (Próximamente)',
      description: 'Aquí elegirás tu mazo, el mazo rival y la dificultad del Bot para iniciar una partida.'
    }
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
