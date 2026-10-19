import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { DocumentCategory, DocumentDetail, DocumentSummary } from '../models/api.models';

const POLL_INTERVAL_MS = 1500;

/** Dokumente des Benutzers inkl. automatischer Aktualisierung, solange Texte extrahiert werden. */
@Injectable({ providedIn: 'root' })
export class DocumentStore {
  private readonly http = inject(HttpClient);
  private readonly state = signal<DocumentSummary[]>([]);
  private pollHandle: ReturnType<typeof setTimeout> | null = null;

  readonly documents = this.state.asReadonly();
  readonly readyDocuments = computed(() => this.state().filter((d) => d.status === 'READY'));
  readonly processing = computed(() => this.state().some((d) => d.status === 'PROCESSING'));

  load(): Observable<DocumentSummary[]> {
    return this.http.get<DocumentSummary[]>('/api/documents').pipe(
      tap((docs) => {
        this.state.set(docs);
        this.schedulePoll();
      }),
    );
  }

  upload(file: File): Observable<DocumentSummary> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<DocumentSummary>('/api/documents', form).pipe(
      tap((doc) => {
        this.state.update((docs) => [...docs, doc]);
        this.schedulePoll();
      }),
    );
  }

  updateCategory(id: string, category: DocumentCategory): Observable<DocumentSummary> {
    return this.http
      .patch<DocumentSummary>(`/api/documents/${id}`, { category })
      .pipe(tap((doc) => this.state.update((docs) => docs.map((d) => (d.id === id ? doc : d)))));
  }

  remove(id: string): Observable<void> {
    return this.http
      .delete<void>(`/api/documents/${id}`)
      .pipe(tap(() => this.state.update((docs) => docs.filter((d) => d.id !== id))));
  }

  detail(id: string): Observable<DocumentDetail> {
    return this.http.get<DocumentDetail>(`/api/documents/${id}`);
  }

  /** Erste Seite als PNG (leerer Blob, wenn das Format keine Seitenvorschau hat). */
  preview(id: string): Observable<Blob> {
    return this.http.get(`/api/documents/${id}/preview`, { responseType: 'blob' });
  }

  clear(): void {
    this.state.set([]);
    this.stopPolling();
  }

  private schedulePoll(): void {
    if (this.pollHandle !== null || !this.processing()) {
      return;
    }
    this.pollHandle = setTimeout(() => {
      this.pollHandle = null;
      this.load().subscribe({ error: () => this.stopPolling() });
    }, POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollHandle !== null) {
      clearTimeout(this.pollHandle);
      this.pollHandle = null;
    }
  }
}
