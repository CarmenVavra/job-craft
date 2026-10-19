import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  WritableSignal,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApplicationService } from '../../core/api/application.service';
import { LetterStyle } from '../../core/models/api.models';
import { ToastService } from '../../core/ui/toast.service';
import { saveBlob } from '../../core/util/download';
import { errorMessage, formatSize, pdfFileName } from '../../core/util/labels';

/** Übergabedaten des PDF-Export-Dialogs. */
export interface PdfExportDialogData {
  applicationId: string;
  version: number;
  company: string | null;
  fullName: string | null;
  /** Gemeinsame Vorlagenwahl mit der Editor-Vorschau. */
  letterStyle: WritableSignal<LetterStyle>;
}

type ExportState = 'idle' | 'gen' | 'done';

/** Dialog „Als PDF exportieren“: Vorlage, Anhänge, Dateiname und Download. */
@Component({
  selector: 'app-pdf-export-dialog',
  templateUrl: './pdf-export-dialog.component.html',
  styleUrl: './pdf-export-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PdfExportDialogComponent {
  protected readonly data = inject<PdfExportDialogData>(DIALOG_DATA);
  private readonly dialogRef = inject(DialogRef);
  private readonly api = inject(ApplicationService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly styles: { value: LetterStyle; label: string }[] = [
    { value: 'CLASSIC', label: 'Klassisch' },
    { value: 'MODERN', label: 'Modern' },
  ];
  protected readonly attachCv = signal(true);
  protected readonly attachCerts = signal(false);
  protected readonly state = signal<ExportState>('idle');
  protected readonly size = signal('');
  protected readonly error = signal('');
  protected readonly fileName = computed(() => pdfFileName(this.data.company, this.data.fullName));

  protected selectStyle(style: LetterStyle): void {
    this.data.letterStyle.set(style);
    this.reset();
  }

  protected toggle(target: 'cv' | 'certs'): void {
    (target === 'cv' ? this.attachCv : this.attachCerts).update((v) => !v);
    this.reset();
  }

  protected download(): void {
    this.state.set('gen');
    this.error.set('');
    this.api
      .pdf(this.data.applicationId, {
        version: this.data.version,
        template: this.data.letterStyle(),
        attachCv: this.attachCv(),
        attachCertificates: this.attachCerts(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          saveBlob(blob, this.fileName());
          this.size.set(formatSize(blob.size));
          this.state.set('done');
          this.toast.show('Anschreiben als PDF heruntergeladen');
        },
        error: (err: unknown) => {
          this.state.set('idle');
          this.error.set(errorMessage(err, 'Das PDF konnte nicht erstellt werden.'));
        },
      });
  }

  protected close(): void {
    this.dialogRef.close();
  }

  private reset(): void {
    if (this.state() === 'done') {
      this.state.set('idle');
    }
  }
}
