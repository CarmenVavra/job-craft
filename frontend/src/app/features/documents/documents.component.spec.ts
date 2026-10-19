import { Dialog } from '@angular/cdk/dialog';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { documentFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { DocumentStore } from '../../core/api/document.store';
import { ToastService } from '../../core/ui/toast.service';
import { DocumentPreviewDialogComponent } from './document-preview-dialog.component';
import { DocumentsComponent } from './documents.component';

describe('DocumentsComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DocumentsComponent], providers: testProviders() });
    http = TestBed.inject(HttpTestingController);
  });

  async function setup() {
    TestBed.inject(DocumentStore).load().subscribe();
    http.expectOne('/api/documents').flush([
      documentFixture(),
      documentFixture({
        id: 'doc-2',
        fileName: 'Skill-Profil.docx',
        category: 'SKILLS',
        contentType: 'x',
      }),
    ]);
    const fixture = TestBed.createComponent(DocumentsComponent);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('lists documents with meta, category and status', async () => {
    const { el } = await setup();
    const rows = el.querySelectorAll('.row');
    expect(rows.length).toBe(2);
    expect(text(rows[0].querySelector('.meta'))).toBe('248 KB · 2 Seiten · 02.10.2026');
    expect(text(rows[0].querySelector('.thumb'))).toBe('PDF');
    expect(text(rows[1].querySelector('.thumb'))).toBe('DOCX');
    expect(text(rows[0].querySelector('.status'))).toBe('Text extrahiert');
  });

  it('filters by category', async () => {
    const { fixture, el } = await setup();
    const filters = el.querySelectorAll('.filter');
    expect(text(filters[2])).toBe('Skill-Profil 1');
    (filters[2] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(el.querySelectorAll('.row').length).toBe(1);
    (filters[4] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(text(el.querySelector('.empty'))).toBe('Keine Dokumente in dieser Kategorie.');
  });

  it('rejects unsupported files before uploading', async () => {
    const { fixture, el } = await setup();
    const dt = new DataTransfer();
    dt.items.add(new File(['x'], 'bild.png'));
    el.querySelector('.dropzone')?.dispatchEvent(new DragEvent('drop', { dataTransfer: dt }));
    await fixture.whenStable();
    expect(text(el.querySelector('[role=alert]'))).toContain('Nur PDF- oder DOCX-Dateien');
    http.expectNone('/api/documents');
  });

  it('uploads dropped PDFs', async () => {
    const { el } = await setup();
    const dt = new DataTransfer();
    dt.items.add(new File(['%PDF'], 'Zeugnis.pdf', { type: 'application/pdf' }));
    el.querySelector('.dropzone')?.dispatchEvent(new DragEvent('drop', { dataTransfer: dt }));
    http.expectOne('/api/documents').flush(documentFixture({ id: 'doc-3', status: 'PROCESSING' }));
    expect(TestBed.inject(DocumentStore).documents().length).toBe(3);
  });

  it('changes the category and deletes with a toast', async () => {
    const { fixture, el } = await setup();
    (el.querySelector('.cat-pill') as HTMLButtonElement).click();
    await fixture.whenStable();
    (el.querySelectorAll('.menu button')[3] as HTMLButtonElement).click();
    http.expectOne('/api/documents/doc-1').flush(documentFixture({ category: 'OTHER' }));

    (el.querySelector('.jc-btn-ghost-danger') as HTMLButtonElement).click();
    http.expectOne('/api/documents/doc-1').flush(null);
    expect(TestBed.inject(ToastService).message()).toBe(
      '„Lebenslauf_Lena_Hoffmann_2026.pdf“ gelöscht',
    );
  });

  it('opens the preview dialog', async () => {
    const { el } = await setup();
    const open = spyOn(TestBed.inject(Dialog), 'open').and.returnValue({} as DialogRef);
    (el.querySelector('.jc-btn-small') as HTMLButtonElement).click();
    expect(open).toHaveBeenCalledWith(DocumentPreviewDialogComponent, jasmine.any(Object));
  });
});

describe('DocumentPreviewDialogComponent', () => {
  it('shows the extracted text and the rendered first page', async () => {
    const createObjectURL = spyOn(URL, 'createObjectURL').and.returnValue('blob:page');
    spyOn(URL, 'revokeObjectURL');
    TestBed.configureTestingModule({
      imports: [DocumentPreviewDialogComponent],
      providers: [
        ...testProviders(),
        { provide: DIALOG_DATA, useValue: documentFixture() },
        { provide: DialogRef, useValue: { close: jasmine.createSpy('close') } },
      ],
    });
    const fixture = TestBed.createComponent(DocumentPreviewDialogComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/documents/doc-1')
      .flush({ ...documentFixture(), extractedText: 'LENA HOFFMANN' });
    http.expectOne('/api/documents/doc-1/preview').flush(new Blob(['png']));
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    expect(text(el.querySelector('.text'))).toBe('LENA HOFFMANN');
    expect(el.querySelector('img')?.getAttribute('src')).toBe('blob:page');
    expect(text(el.querySelector('.titles p'))).toContain('Lebenslauf · 248 KB');
    expect(createObjectURL).toHaveBeenCalled();
  });
});
