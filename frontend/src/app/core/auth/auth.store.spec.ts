import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard, guestGuard } from './auth.guard';
import { AuthStore } from './auth.store';
import { jwtInterceptor } from './jwt.interceptor';

describe('AuthStore, jwtInterceptor, and guards', () => {
  let store: AuthStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting()
      ]
    });
    store = TestBed.inject(AuthStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('logs in, persists token, and attaches Bearer token on /api requests', () => {
    store.login({ identifier: 'ash', password: 'secret' });

    const loginReq = httpMock.expectOne('/api/auth/login');
    expect(loginReq.request.method).toBe('POST');
    expect(loginReq.request.headers.has('Authorization')).toBeFalse();
    loginReq.flush({
      token: 'jwt-123',
      user: {
        id: 'u1',
        username: 'ash',
        email: 'ash@pallet.town',
        role: 'USER',
        matchesPlayed: 0,
        matchesWon: 0,
        favoriteCardIds: [],
        createdAt: '2026-11-23T00:00:00Z'
      }
    });

    expect(store.isAuthenticated()).toBeTrue();
    expect(store.token()).toBe('jwt-123');

    store.toggleFavorite('base1-4');
    const favReq = httpMock.expectOne('/api/users/me/favorites/base1-4');
    expect(favReq.request.method).toBe('PUT');
    expect(favReq.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    favReq.flush({
      id: 'u1',
      username: 'ash',
      email: 'ash@pallet.town',
      role: 'USER',
      matchesPlayed: 0,
      matchesWon: 0,
      favoriteCardIds: ['base1-4'],
      createdAt: '2026-11-23T00:00:00Z'
    });

    expect(store.favoriteCardIds().has('base1-4')).toBeTrue();
  });

  it('guards protected and guest routes based on auth state', () => {
    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/decks' } as RouterStateSnapshot;

    const unauthResult = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(unauthResult instanceof UrlTree).toBeTrue();
    expect((unauthResult as UrlTree).toString()).toContain('/login');

    const guestAllowed = TestBed.runInInjectionContext(() => guestGuard(route, state));
    expect(guestAllowed).toBeTrue();

    store.token.set('valid-token');
    expect(TestBed.runInInjectionContext(() => authGuard(route, state))).toBeTrue();
    expect(TestBed.runInInjectionContext(() => guestGuard(route, state)) instanceof UrlTree).toBeTrue();
  });
});
