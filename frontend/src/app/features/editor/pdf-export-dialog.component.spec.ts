import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { testProviders, text } from '../../../testing/providers';
import { LetterStyle } from '../../core/models/api.models';
import { PdfExportDialogComponent, PdfExportDialogData } from './pdf-export-dialog.component';

describe('PdfExportDialogComponent', () => {
  const letterStyle = signal<LetterStyle>('CLASSIC');
  const dialogRef = { close: jasmine.createSpy('close') };

  beforeEach(() => {
    letterStyle.set('CLASSIC');
    const data: PdfExportDialogData = {
      applicationId: 'app-1',
      version: 2,
      company: 'Quantis Software GmbH',
      fullName: 'Lena Hoffmann',
      letterStyle,
    };
    TestBed.configureTestingModule({
      imports: [PdfExportDialogComponent],
      providers: [
        ...testProviders(),
        { provide: DIALOG_DATA, useValue: data },
        { provide: DialogRef, useValue: dialogRef },
      ],
    });
  });

  it('shows file name and default options', async () => {
    const fixture = TestBed.createComponent(PdfExportDialogComponent);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el.querySelector('.filename'))).toBe(
      'Bewerbung_Quantis_Software_GmbH_Hoffmann.pdf',
    );
    expect(el.querySelectorAll('.switch.on').length).toBe(1);
    expect(text(el.querySelector('.template.active'))).toBe('Klassisch');
  });

  it('switches the template for the editor preview', async () => {
    const fixture = TestBed.createComponent(PdfExportDialogComponent);
    await fixture.whenStable();
    (fixture.nativeElement.querySelectorAll('.template')[1] as HTMLButtonElement).click();
    expect(letterStyle()).toBe('MODERN');
  });

  it('downloads the PDF with the selected options', async () => {
    const fixture = TestBed.createComponent(PdfExportDialogComponent);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    spyOn(HTMLAnchorElement.prototype, 'click');
    (el.querySelectorAll('.toggle-row')[1] as HTMLButtonElement).click();
    (el.querySelector('.cta') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(text(el.querySelector('.generating'))).toBe('PDF wird gerendert …');

    const req = TestBed.inject(HttpTestingController).expectOne(
      (r) => r.url === '/api/applications/app-1/pdf',
    );
    expect(req.request.params.get('attachCertificates')).toBe('true');
    expect(req.request.params.get('version')).toBe('2');
    req.flush(new Blob([new Uint8Array(2048)]));
    await fixture.whenStable();

    expect(text(el.querySelector('.done'))).toContain('✓ PDF bereit · 2 KB');
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();
  });
});
