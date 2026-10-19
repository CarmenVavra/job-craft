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
});
