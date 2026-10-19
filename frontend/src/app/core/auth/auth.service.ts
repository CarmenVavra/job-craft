import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { DocumentStore } from '../api/document.store';
import { ProfileStore } from '../api/profile.store';
import {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  User,
} from '../models/api.models';

const TOKEN_KEY = 'jobcraft.token';
const USER_KEY = 'jobcraft.user';

/** Anmeldung, Registrierung und Sitzungszustand (JWT im localStorage). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly profileStore = inject(ProfileStore);
  private readonly documentStore = inject(DocumentStore);

  private readonly tokenState = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly userState = signal<User | null>(parseUser(readStorage(USER_KEY)));

  readonly token = this.tokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => {
    const token = this.tokenState();
    return token !== null && !isExpired(token);
  });

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', request)
      .pipe(tap((res) => this.setSession(res)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', request)
      .pipe(tap((res) => this.setSession(res)));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>('/api/auth/password', request);
  }

  /** Aktualisiert den angezeigten Namen (z. B. nach dem Speichern des Briefkopfs). */
  updateUserName(name: string): void {
    const user = this.userState();
    if (user) {
      this.setUser({ ...user, name });
    }
  }

  logout(): void {
    this.tokenState.set(null);
    this.userState.set(null);
    removeStorage(TOKEN_KEY);
    removeStorage(USER_KEY);
    this.profileStore.clear();
    this.documentStore.clear();
    void this.router.navigate(['/login']);
  }

  private setSession(res: AuthResponse): void {
    this.tokenState.set(res.token);
    writeStorage(TOKEN_KEY, res.token);
    this.setUser(res.user);
  }

  private setUser(user: User): void {
    this.userState.set(user);
    writeStorage(USER_KEY, JSON.stringify(user));
  }
}

function isExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as {
      exp?: number;
    };
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

function parseUser(raw: string | null): User | null {
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* Speicher nicht verfügbar (z. B. privater Modus) – Sitzung bleibt im Speicher. */
  }
}

function removeStorage(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignorieren */
  }
}
