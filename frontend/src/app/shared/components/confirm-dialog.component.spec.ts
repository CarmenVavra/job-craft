import { DIALOG_DATA, Dialog, DialogRef } from '@angular/cdk/dialog';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { testProviders, text } from '../../../testing/providers';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  const dialogRef = { close: jasmine.createSpy('close') };
  const data: ConfirmDialogData = {
    title: 'Bewerbung löschen?',
    message: 'Wird endgültig gelöscht.',
    confirmLabel: 'Löschen',
    danger: true,
  };

  beforeEach(() => {
    dialogRef.close.calls.reset();
    TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      providers: [
        ...testProviders(),
        { provide: DIALOG_DATA, useValue: data },
        { provide: DialogRef, useValue: dialogRef },
      ],
    });
  });

  it('shows title, message and a red confirm button', async () => {
    const fixture = TestBed.createComponent(ConfirmDialogComponent);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el.querySelector('h2'))).toBe('Bewerbung löschen?');
    expect(text(el.querySelector('p'))).toBe('Wird endgültig gelöscht.');
    expect(el.querySelector('.confirm-btn')?.classList).toContain('jc-btn-danger');
    expect(text(el.querySelector('.cancel'))).toBe('Abbrechen');
  });

  it('closes with true on confirm and false on cancel', async () => {
    const fixture = TestBed.createComponent(ConfirmDialogComponent);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    (el.querySelector('.confirm-btn') as HTMLButtonElement).click();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
    (el.querySelector('.cancel') as HTMLButtonElement).click();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});

describe('ConfirmService', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: testProviders() }));

  it('resolves to true only when the dialog is confirmed', async () => {
    const service = TestBed.inject(ConfirmService);
    const dialog = TestBed.inject(Dialog);

    const confirmed = firstValueFrom(
      service.confirm({ title: 'T', message: 'M', confirmLabel: 'Ja' }),
    );
    dialog.openDialogs[0].close(true);
    expect(await confirmed).toBeTrue();

    const dismissed = firstValueFrom(
      service.confirm({ title: 'T', message: 'M', confirmLabel: 'Ja' }),
    );
    dialog.openDialogs[0].close();
    expect(await dismissed).toBeFalse();
  });
});
