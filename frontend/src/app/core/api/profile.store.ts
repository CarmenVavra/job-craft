import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Profile } from '../models/api.models';

/** Briefkopf-Stammdaten als geteilter Signal-Zustand. */
@Injectable({ providedIn: 'root' })
export class ProfileStore {
  private readonly http = inject(HttpClient);
  private readonly state = signal<Profile | null>(null);

  readonly profile = this.state.asReadonly();

  load(): Observable<Profile> {
    return this.http.get<Profile>('/api/profile').pipe(tap((p) => this.state.set(p)));
  }

  save(profile: Profile): Observable<Profile> {
    return this.http.put<Profile>('/api/profile', profile).pipe(tap((p) => this.state.set(p)));
  }

  clear(): void {
    this.state.set(null);
  }
}
