import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { filter, switchMap } from 'rxjs';
import { NoteStore } from '../../core/api/note.store';
import { ProfileNote } from '../../core/models/api.models';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import { errorMessage, formatDate } from '../../core/util/labels';

export const MAX_TITLE = 120;
export const MAX_CONTENT = 4000;

/** Vorschläge für den Einstieg; ein Klick öffnet den Editor mit passendem Titel. */
const SUGGESTIONS = ['Privatprojekt', 'Weiterbildung', 'Ehrenamt', 'Motivation & Ziele'];

/**
 * „Über mich“: weitere Angaben, die nicht im Lebenslauf stehen (z. B. Privatprojekte). Aktive Angaben
 * berücksichtigt die KI beim Anforderungsabgleich und beim Schreiben des Anschreibens.
 */
@Component({
  selector: 'app-about',
  imports: [FormsModule, NgTemplateOutlet],
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AboutComponent {
  private readonly store = inject(NoteStore);
  private readonly toast = inject(ToastService);
  private readonly confirmService = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly maxTitle = MAX_TITLE;
  protected readonly maxContent = MAX_CONTENT;
  protected readonly suggestions = SUGGESTIONS;

  protected readonly notes = this.store.notes;
  protected readonly loading = signal(true);

  /** ID der bearbeiteten Angabe, {@code 'new'} für eine neue oder {@code null}. */
  protected readonly editing = signal<string | null>(null);
  protected readonly draftTitle = signal('');
  protected readonly draftContent = signal('');
  protected readonly draftIncluded = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  constructor() {
    this.store
      .load()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.loading.set(false),
        error: (err: unknown) => {
          this.loading.set(false);
          this.error.set(errorMessage(err));
        },
      });
  }

  protected startNew(title = ''): void {
    this.editing.set('new');
    this.draftTitle.set(title ? `${title}: ` : '');
    this.draftContent.set('');
    this.draftIncluded.set(true);
    this.error.set('');
  }

  protected startEdit(note: ProfileNote): void {
    this.editing.set(note.id);
    this.draftTitle.set(note.title);
    this.draftContent.set(note.content);
    this.draftIncluded.set(note.included);
    this.error.set('');
  }

  protected cancel(): void {
    this.editing.set(null);
    this.error.set('');
  }

  protected save(): void {
    const title = this.draftTitle().trim();
    const content = this.draftContent().trim();
    if (!title || !content) {
      this.error.set('Bitte gib einen Titel und eine Beschreibung ein.');
      return;
    }
    const id = this.editing();
    const request = { title, content, included: this.draftIncluded() };
    this.saving.set(true);
    this.error.set('');
    (id === 'new' || id === null ? this.store.create(request) : this.store.update(id, request))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editing.set(null);
          this.toast.show(`„${title}“ gespeichert`);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(errorMessage(err));
        },
      });
  }

  protected toggleIncluded(note: ProfileNote): void {
    this.store
      .update(note.id, { title: note.title, content: note.content, included: !note.included })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) =>
          this.toast.show(
            saved.included
              ? `„${saved.title}“ wird berücksichtigt`
              : `„${saved.title}“ wird nicht mehr berücksichtigt`,
          ),
        error: (err: unknown) => this.toast.show(errorMessage(err)),
      });
  }

  protected remove(note: ProfileNote): void {
    this.confirmService
      .confirm({
        title: 'Angabe löschen?',
        message: `„${note.title}“ wird endgültig gelöscht und bei neuen Anschreiben nicht mehr berücksichtigt.`,
        confirmLabel: 'Löschen',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.store.remove(note.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          if (this.editing() === note.id) {
            this.editing.set(null);
          }
          this.toast.show(`„${note.title}“ gelöscht`);
        },
        error: (err: unknown) => this.toast.show(errorMessage(err)),
      });
  }

  protected changed(note: ProfileNote): string {
    return `Zuletzt geändert am ${formatDate(note.updatedAt ?? note.createdAt)}`;
  }
}
