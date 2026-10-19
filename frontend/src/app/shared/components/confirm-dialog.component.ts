import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

/** Inhalt des Bestätigungsdialogs. */
export interface ConfirmDialogData {
  title: string;
  message: string;
  /** Beschriftung der bestätigenden Schaltfläche, z. B. „Löschen“. */
  confirmLabel: string;
  cancelLabel?: string;
  /** Rote Schaltfläche für unwiderrufliche Aktionen. */
  danger?: boolean;
}

/** Bestätigungsdialog im JobCraft-Stil; schließt mit {@code true} (bestätigt) oder {@code false}. */
@Component({
  selector: 'app-confirm-dialog',
  template: `
    <div
      class="jc-dialog confirm"
      role="alertdialog"
      aria-labelledby="confirm-title"
      aria-describedby="confirm-message"
    >
      <h2 id="confirm-title">{{ data.title }}</h2>
      <p id="confirm-message">{{ data.message }}</p>
      <div class="actions">
        <button type="button" class="jc-btn jc-btn-secondary cancel" (click)="close(false)">
          {{ data.cancelLabel ?? 'Abbrechen' }}
        </button>
        <button
          type="button"
          class="jc-btn confirm-btn"
          [class.jc-btn-danger]="data.danger"
          [class.jc-btn-primary]="!data.danger"
          (click)="close(true)"
        >
          {{ data.confirmLabel }}
        </button>
      </div>
    </div>
  `,
  styles: `
    .confirm {
      padding: 26px 28px 24px;
      gap: 10px;
    }
    h2 {
      margin: 0;
      font-size: 19px;
      font-weight: 600;
      letter-spacing: -0.01em;
    }
    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.55;
      color: var(--jc-text-2);
      text-wrap: pretty;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 14px;
    }
    .cancel {
      padding: 10px 16px;
      font: 600 14px var(--jc-font);
    }
    .confirm-btn {
      padding: 10px 18px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmDialogData>(DIALOG_DATA);
  private readonly dialogRef = inject<DialogRef<boolean>>(DialogRef);

  protected close(result: boolean): void {
    this.dialogRef.close(result);
  }
}
