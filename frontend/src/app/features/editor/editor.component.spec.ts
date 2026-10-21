import { Dialog } from '@angular/cdk/dialog';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { applicationFixture, profileFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { ProfileStore } from '../../core/api/profile.store';
import { EditorComponent } from './editor.component';
import { PdfExportDialogComponent } from './pdf-export-dialog.component';

describe('EditorComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EditorComponent], providers: testProviders() });
    http = TestBed.inject(HttpTestingController);
  });

  async function setup() {
    TestBed.inject(ProfileStore).load().subscribe();
    http.expectOne('/api/profile').flush(profileFixture({ letterStyle: 'MODERN' }));
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentRef.setInput('id', 'app-1');
    fixture.detectChanges();
    http.expectOne('/api/applications/app-1').flush(applicationFixture());
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows the latest version with toolbar, letter and chat', async () => {
    const { el } = await setup();
    expect(text(el.querySelector('.company'))).toBe('Quantis Software GmbH');
    expect(el.querySelectorAll('.version-chip').length).toBe(2);
    expect(text(el.querySelector('.version-chip.active'))).toBe('v2');
    expect(text(el.querySelectorAll('.paragraph')[1])).toBe('Absatz zwei neu');
    expect(el.querySelector('.head-modern')).not.toBeNull();
    expect(text(el.querySelector('.chat-settings'))).toBe('Formell · Standard');
    expect(text(el.querySelector('.meta'))).toBe('v1 erstellt');
    expect(text(el.querySelectorAll('.suggestion')[4])).toBe('Kubernetes-Lücke adressieren');
  });

  it('shows a banner for older versions', async () => {
    const { fixture, el } = await setup();
    (el.querySelectorAll('.version-chip')[0] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(text(el.querySelector('.old-banner'))).toContain('Du siehst eine ältere Version (v1).');
    expect(text(el.querySelectorAll('.paragraph')[1])).toBe('Absatz zwei');

    (el.querySelector('.to-latest') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(el.querySelector('.old-banner')).toBeNull();
  });

  it('sends a chat message and highlights the changed paragraph', async () => {
    const { fixture, el } = await setup();
    (el.querySelectorAll('.suggestion')[3] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(el.querySelector('.typing')).not.toBeNull();
    expect(text(el.querySelectorAll('.message.user .bubble')[0])).toBe('Einleitung kürzen');

    const req = http.expectOne('/api/applications/app-1/chat');
    expect(req.request.body).toEqual({ message: 'Einleitung kürzen' });
    const updated = applicationFixture();
    updated.versions = [
      ...updated.versions,
      {
        version: 3,
        paragraphs: ['Kurz.', 'Absatz zwei neu'],
        changedParagraphs: [0],
        createdAt: '',
      },
    ];
    req.flush(updated);
    await fixture.whenStable();

    expect(el.querySelector('.typing')).toBeNull();
    expect(text(el.querySelector('.version-chip.active'))).toBe('v3');
    expect(el.querySelector('.paragraph.changed')?.textContent).toBe('Kurz.');
  });

  it('opens the PDF dialog', async () => {
    const { el } = await setup();
    const open = spyOn(TestBed.inject(Dialog), 'open').and.callThrough();
    (el.querySelector('.export') as HTMLButtonElement).click();
    expect(open).toHaveBeenCalledWith(PdfExportDialogComponent, jasmine.objectContaining({}));
    TestBed.inject(Dialog).closeAll();
  });

  it('saves a directly edited paragraph as a new version', async () => {
    const { fixture, el } = await setup();
    (el.querySelectorAll('.paragraph-tools .tool')[0] as HTMLButtonElement).click();
    await fixture.whenStable();
    const area = el.querySelector('.edit-area') as HTMLTextAreaElement;
    area.value = 'Absatz eins geändert';
    area.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    (el.querySelector('.edit-save') as HTMLButtonElement).click();

    const req = http.expectOne('/api/applications/app-1/paragraphs');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ paragraphs: ['Absatz eins geändert', 'Absatz zwei neu'] });
    const updated = applicationFixture();
    updated.versions = [
      ...updated.versions,
      {
        version: 3,
        paragraphs: ['Absatz eins geändert', 'Absatz zwei neu'],
        changedParagraphs: [0],
        createdAt: '',
      },
    ];
    req.flush(updated);
    await fixture.whenStable();

    expect(text(el.querySelector('.version-chip.active'))).toBe('v3');
    expect(el.querySelector('.edit-area')).toBeNull();
    expect(el.querySelector('.paragraph.changed')?.textContent).toBe('Absatz eins geändert');
  });

  it('saves a custom salutation and can switch back to the automatic one', async () => {
    const { fixture, el } = await setup();
    (el.querySelector('.salutation-tools .tool') as HTMLButtonElement).click();
    await fixture.whenStable();
    const input = el.querySelector('.salutation-input') as HTMLInputElement;
    input.value = 'Liebe Frau Kessler,';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    (el.querySelector('.edit-save') as HTMLButtonElement).click();

    const req = http.expectOne('/api/applications/app-1/salutation');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ salutation: 'Liebe Frau Kessler,' });
    const updated = applicationFixture({ customSalutation: 'Liebe Frau Kessler,' });
    updated.letter = { ...updated.letter, salutation: 'Liebe Frau Kessler,' };
    req.flush(updated);
    await fixture.whenStable();
    expect(text(el.querySelector('.salutation'))).toBe('Liebe Frau Kessler,');
    expect(text(el.querySelector('.version-chip.active'))).toBe('v2');

    (el.querySelectorAll('.salutation-tools .tool')[1] as HTMLButtonElement).click();
    expect(http.expectOne('/api/applications/app-1/salutation').request.body).toEqual({
      salutation: null,
    });
  });

  it('deletes a paragraph and disables editing for older versions', async () => {
    const { fixture, el } = await setup();
    (el.querySelectorAll('.paragraph-tools .danger')[0] as HTMLButtonElement).click();
    const req = http.expectOne('/api/applications/app-1/paragraphs');
    expect(req.request.body).toEqual({ paragraphs: ['Absatz zwei neu'] });
    req.flush(applicationFixture());
    await fixture.whenStable();

    (el.querySelectorAll('.version-chip')[0] as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(el.querySelector('.paragraph-tools')).toBeNull();
  });
});
