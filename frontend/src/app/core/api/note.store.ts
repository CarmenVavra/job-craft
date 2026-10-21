import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ProfileNote, ProfileNoteRequest } from '../models/api.models';

/** Weitere Angaben zur Person („Über mich“) als geteilter Signal-Zustand. */
@Injectable({ providedIn: 'root' })
export class NoteStore {
  private readonly http = inject(HttpClient);
  private readonly state = signal<ProfileNote[]>([]);

  readonly notes = this.state.asReadonly();
  readonly includedNotes = computed(() => this.state().filter((n) => n.included));

  load(): Observable<ProfileNote[]> {
    return this.http.get<ProfileNote[]>('/api/notes').pipe(tap((notes) => this.state.set(notes)));
  }

  create(request: ProfileNoteRequest): Observable<ProfileNote> {
    return this.http
      .post<ProfileNote>('/api/notes', request)
      .pipe(tap((note) => this.state.update((notes) => [...notes, note])));
  }

  update(id: string, request: ProfileNoteRequest): Observable<ProfileNote> {
    return this.http
      .put<ProfileNote>(`/api/notes/${id}`, request)
      .pipe(
        tap((note) => this.state.update((notes) => notes.map((n) => (n.id === id ? note : n)))),
      );
  }

  remove(id: string): Observable<void> {
    return this.http
      .delete<void>(`/api/notes/${id}`)
      .pipe(tap(() => this.state.update((notes) => notes.filter((n) => n.id !== id))));
  }
}
