import { Injectable, signal } from '@angular/core';

const TOAST_DURATION_MS = 2600;

/** Kurze Rückmeldung unten mittig (z. B. „Briefkopf gespeichert“). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly state = signal<string | null>(null);
  private handle: ReturnType<typeof setTimeout> | null = null;

  readonly message = this.state.asReadonly();

  show(message: string): void {
    this.state.set(message);
    if (this.handle !== null) {
      clearTimeout(this.handle);
    }
    this.handle = setTimeout(() => {
      this.state.set(null);
      this.handle = null;
    }, TOAST_DURATION_MS);
  }
}
