import { Dialog } from '@angular/cdk/dialog';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog.component';

/** Öffnet den Bestätigungsdialog; Abbrechen, Esc und Klick daneben gelten als „nein“. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  confirm(data: ConfirmDialogData): Observable<boolean> {
    const ref = this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialogComponent, {
      data,
      width: '420px',
      maxWidth: 'calc(100vw - 48px)',
      panelClass: 'jc-dialog-panel',
      backdropClass: 'jc-dialog-backdrop',
      ariaLabelledBy: 'confirm-title',
    });
    return ref.closed.pipe(map((result) => result === true));
  }
}
