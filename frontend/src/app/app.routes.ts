import { Routes } from '@angular/router';

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
    loadComponent: () =>
      import('./shared/ui/feature-placeholder/feature-placeholder.component').then(
        (m) => m.FeaturePlaceholderComponent
      ),
    data: {
      phase: 'Fase 2',
      title: 'Deck Builder (Próximamente)',
      description: 'Aquí podrás crear, editar y validar tus mazos de 60 cartas y clonar starter decks.'
    }
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
    loadComponent: () =>
      import('./shared/ui/feature-placeholder/feature-placeholder.component').then(
        (m) => m.FeaturePlaceholderComponent
      ),
    data: {
      phase: 'Fase 2',
      title: 'Iniciar Sesión (Próximamente)',
      description: 'La autenticación JWT y perfil de entrenador estarán disponibles en la Fase 2.'
    }
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./shared/ui/feature-placeholder/feature-placeholder.component').then(
        (m) => m.FeaturePlaceholderComponent
      ),
    data: {
      phase: 'Fase 2',
      title: 'Registro de Entrenador (Próximamente)',
      description: 'El registro de nuevas cuentas estará disponible en la Fase 2.'
    }
  },
  {
    path: '**',
    redirectTo: ''
  }
];
