import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DocumentStore } from '../../core/api/document.store';
import { DocumentDetail, DocumentSummary } from '../../core/models/api.models';
import { CATEGORY_LABELS, errorMessage, formatDate, formatSize } from '../../core/util/labels';

/** Vorschau eines Dokuments: links die erste Seite, rechts der extrahierte Text. */
@Component({
  selector: 'app-document-preview-dialog',
  templateUrl: './document-preview-dialog.component.html',
  styleUrl: './document-preview-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentPreviewDialogComponent implements OnInit {
  protected readonly document = inject<DocumentSummary>(DIALOG_DATA);
  private readonly dialogRef = inject(DialogRef);
  private readonly store = inject(DocumentStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly formatSize = formatSize;
  protected readonly formatDate = formatDate;
  protected readonly detail = signal<DocumentDetail | null>(null);
  protected readonly pageUrl = signal<string | null>(null);
  protected readonly error = signal('');

  constructor() {
    this.destroyRef.onDestroy(() => {
      const url = this.pageUrl();
      if (url) URL.revokeObjectURL(url);
    });
  }

  ngOnInit(): void {
    this.store
      .detail(this.document.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (d) => this.detail.set(d),
        error: (err: unknown) => this.error.set(errorMessage(err)),
      });
    this.store
      .preview(this.document.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => this.pageUrl.set(blob.size > 0 ? URL.createObjectURL(blob) : null),
        error: () => this.pageUrl.set(null),
      });
  }

  protected close(): void {
    this.dialogRef.close();
  }
}
