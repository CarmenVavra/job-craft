import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DocumentStore } from '../../core/api/document.store';
import { DocumentCategory, DocumentSummary } from '../../core/models/api.models';
import { ToastService } from '../../core/ui/toast.service';
import {
  CATEGORIES,
  CATEGORY_COLORS,
  CATEGORY_LABELS,
  errorMessage,
  formatDate,
  formatSize,
} from '../../core/util/labels';
import { DocumentPreviewDialogComponent } from './document-preview-dialog.component';

type Filter = 'ALL' | DocumentCategory;

const MAX_SIZE = 10 * 1024 * 1024;
const ALLOWED = /\.(pdf|docx)$/i;

/** Dokumentverwaltung: Upload per Dropzone, Filter, Kategorie, Vorschau und Löschen. */
@Component({
  selector: 'app-documents',
  templateUrl: './documents.component.html',
  styleUrl: './documents.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'categoryMenuFor.set(null)',
    '(document:keydown.escape)': 'categoryMenuFor.set(null)',
  },
})
export class DocumentsComponent {
  private readonly store = inject(DocumentStore);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(Dialog);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly categories = CATEGORIES;
  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly categoryColors = CATEGORY_COLORS;

  protected readonly filter = signal<Filter>('ALL');
  protected readonly dragging = signal(false);
  protected readonly uploadError = signal('');
  protected readonly categoryMenuFor = signal<string | null>(null);

  protected readonly documents = this.store.documents;
  protected readonly filters = computed(() => {
    const docs = this.documents();
    const all: { id: Filter; label: string; count: number }[] = [
      { id: 'ALL', label: 'Alle', count: docs.length },
    ];
    return all.concat(
      CATEGORIES.map((c) => ({
        id: c as Filter,
        label: CATEGORY_LABELS[c],
        count: docs.filter((d) => d.category === c).length,
      })),
    );
  });
  protected readonly visible = computed(() => {
    const f = this.filter();
    return this.documents().filter((d) => f === 'ALL' || d.category === f);
  });

  protected extension(doc: DocumentSummary): string {
    return (doc.fileName.split('.').pop() ?? '').toUpperCase();
  }

  protected meta(doc: DocumentSummary): string {
    const parts = [formatSize(doc.sizeBytes)];
    if (doc.pageCount) {
      parts.push(`${doc.pageCount} ${doc.pageCount === 1 ? 'Seite' : 'Seiten'}`);
    }
    parts.push(formatDate(doc.createdAt));
    return parts.join(' · ');
  }

  protected onFiles(input: HTMLInputElement): void {
    if (input.files) {
      this.upload(Array.from(input.files));
    }
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.upload(Array.from(event.dataTransfer?.files ?? []));
  }

  protected toggleCategoryMenu(event: Event, id: string): void {
    event.stopPropagation();
    this.categoryMenuFor.update((current) => (current === id ? null : id));
  }

  protected setCategory(event: Event, doc: DocumentSummary, category: DocumentCategory): void {
    event.stopPropagation();
    this.categoryMenuFor.set(null);
    if (doc.category === category) {
      return;
    }
    this.store
      .updateCategory(doc.id, category)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: (err: unknown) => this.toast.show(errorMessage(err)) });
  }

  protected preview(doc: DocumentSummary): void {
    this.dialog.open(DocumentPreviewDialogComponent, {
      data: doc,
      width: '820px',
      maxWidth: 'calc(100vw - 48px)',
      panelClass: 'jc-dialog-panel',
      backdropClass: 'jc-dialog-backdrop',
      ariaLabelledBy: 'preview-title',
    });
  }

  protected remove(doc: DocumentSummary): void {
    this.store
      .remove(doc.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.toast.show(`„${doc.fileName}“ gelöscht`),
        error: (err: unknown) => this.toast.show(errorMessage(err)),
      });
  }

  private upload(files: File[]): void {
    this.uploadError.set('');
    for (const file of files) {
      if (!ALLOWED.test(file.name)) {
        this.uploadError.set(`„${file.name}“: Nur PDF- oder DOCX-Dateien sind erlaubt.`);
        continue;
      }
      if (file.size > MAX_SIZE) {
        this.uploadError.set(`„${file.name}“ ist größer als 10 MB.`);
        continue;
      }
      this.filter.set('ALL');
      this.store
        .upload(file)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          error: (err: unknown) =>
            this.uploadError.set(`„${file.name}“: ${errorMessage(err, 'Upload fehlgeschlagen.')}`),
        });
    }
  }
}
