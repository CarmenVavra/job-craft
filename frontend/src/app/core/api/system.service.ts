import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { SystemInfo } from '../models/api.models';

/** Laufzeitinfos des Backends (z. B. ob ein Sprachmodell aktiv ist). */
@Injectable({ providedIn: 'root' })
export class SystemService {
  private readonly http = inject(HttpClient);
  private readonly state = signal<SystemInfo | null>(null);
  private requested = false;

  readonly info = this.state.asReadonly();

  load(): void {
    if (this.requested) {
      return;
    }
    this.requested = true;
    this.http.get<SystemInfo>('/api/system').subscribe({
      next: (info) => this.state.set(info),
      error: () => (this.requested = false),
    });
  }
}
