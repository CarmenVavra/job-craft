import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { applicationFixture, documentFixture, noteFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { DocumentStore } from '../../core/api/document.store';
import { NoteStore } from '../../core/api/note.store';
import { NewApplicationComponent } from './new-application.component';

describe('NewApplicationComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NewApplicationComponent],
      providers: testProviders(),
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('shows the URL step and validates the link', async () => {
    const fixture = TestBed.createComponent(NewApplicationComponent);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el.querySelector('h1'))).toBe('Welche Stelle reizt dich?');

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(text(el.querySelector('.url-error'))).toContain('gültigen Link');
    http.expectNone('/api/applications');
  });

  it('runs the analysis and shows the review step', async () => {
    jasmine.clock().install();
    try {
      const fixture = TestBed.createComponent(NewApplicationComponent);
      fixture.componentRef.setInput('url', 'https://karriere.example.de/jobs/1');
      fixture.detectChanges();

      http
        .expectOne('/api/applications')
        .flush(applicationFixture({ status: 'ANALYZING', analysisStep: 0, versions: [] }));
      jasmine.clock().tick(1);
      http
        .expectOne('/api/applications/app-1')
        .flush(applicationFixture({ status: 'ANALYZED', versions: [] }));
      fixture.detectChanges();
      expect(text(fixture.nativeElement.querySelector('h1'))).toBe(
        'Stellenanzeige wird analysiert',
      );

      jasmine.clock().tick(650 * 6);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(text(el.querySelector('h1'))).toBe('Eckdaten prüfen');
      expect(text(el.querySelector('.match'))).toBe('1 von 2 Anforderungen erfüllt');
      expect(el.querySelectorAll('.chip').length).toBe(2);
    } finally {
      jasmine.clock().uninstall();
    }
  });

  it('generates the letter with the selected context and opens the editor', async () => {
    const store = TestBed.inject(DocumentStore);
    store.load().subscribe();
    http.expectOne('/api/documents').flush([documentFixture()]);
    TestBed.inject(NoteStore).load().subscribe();
    http
      .expectOne('/api/notes')
      .flush([noteFixture(), noteFixture({ id: 'note-2', included: false })]);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    const fixture = TestBed.createComponent(NewApplicationComponent);
    fixture.componentRef.setInput('id', 'app-1');
    fixture.detectChanges();
    http
      .expectOne('/api/applications/app-1')
      .flush(applicationFixture({ status: 'ANALYZED', versions: [] }));
    await fixture.whenStable();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.doc .box.on')).not.toBeNull();
    expect(text(el.querySelector('.notes-hint'))).toBe(
      'Zusätzlich fließt 1 Angabe aus „Über mich“ ein.',
    );
    (
      el.querySelectorAll('.jc-segmented')[0].querySelectorAll('button')[1] as HTMLButtonElement
    ).click();
    (el.querySelector('.generate') as HTMLButtonElement).click();

    const req = http.expectOne('/api/applications/app-1/generate');
    expect(req.request.body.tone).toBe('MODERN');
    expect(req.request.body.documentIds).toEqual(['doc-1']);
    expect(req.request.body.companyName).toBe('Quantis Software GmbH');
    req.flush(applicationFixture());
    expect(navigate).toHaveBeenCalledWith(['/bewerbung', 'app-1']);
  });

  it('shows analysis errors with a retry option', async () => {
    const fixture = TestBed.createComponent(NewApplicationComponent);
    fixture.componentRef.setInput('id', 'app-1');
    fixture.detectChanges();
    http.expectOne('/api/applications/app-1').flush(
      applicationFixture({
        status: 'ANALYSIS_FAILED',
        analysisStep: 0,
        errorMessage: 'Die Stellenanzeige konnte nicht abgerufen werden.',
        versions: [],
      }),
    );
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el.querySelector('[role=alert]'))).toBe(
      'Die Stellenanzeige konnte nicht abgerufen werden.',
    );
    expect(text(el.querySelector('.actions .jc-btn-primary'))).toBe('Erneut versuchen');
  });
});
