import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { noteFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { NoteStore } from '../../core/api/note.store';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import { AboutComponent } from './about.component';

describe('AboutComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AboutComponent], providers: testProviders() });
    http = TestBed.inject(HttpTestingController);
  });

  async function setup(notes = [noteFixture()]) {
    const fixture = TestBed.createComponent(AboutComponent);
    http.expectOne('/api/notes').flush(notes);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  async function type(
    fixture: { whenStable(): Promise<unknown> },
    el: Element | null,
    value: string,
  ) {
    const input = el as HTMLInputElement | HTMLTextAreaElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('lists notes with their state', async () => {
    const { el } = await setup([noteFixture(), noteFixture({ id: 'note-2', included: false })]);
    const notes = el.querySelectorAll('.note');
    expect(notes.length).toBe(2);
    expect(text(notes[0].querySelector('h3'))).toBe('Privatprojekt: Haushaltsbuch-App');
    expect(text(notes[0].querySelector('.include-pill'))).toBe('✓ Wird berücksichtigt');
    expect(notes[1].classList).toContain('paused');
    expect(text(notes[0].querySelector('.meta'))).toBe('Zuletzt geändert am 21.10.2026');
  });

  it('offers suggestions when empty and creates a note', async () => {
    const { fixture, el } = await setup([]);
    const suggestions = el.querySelectorAll('.suggestions button');
    expect(suggestions.length).toBe(4);
    (suggestions[0] as HTMLButtonElement).click();
    await fixture.whenStable();

    const title = el.querySelector('input[name=title]') as HTMLInputElement;
    expect(title.value).toBe('Privatprojekt: ');
    await type(fixture, title, 'Privatprojekt: Wetter-App');
    await type(fixture, el.querySelector('textarea'), 'Ionic, offlinefähig');
    expect(text(el.querySelector('.counter'))).toBe('19 / 4000');
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();

    const req = http.expectOne('/api/notes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      title: 'Privatprojekt: Wetter-App',
      content: 'Ionic, offlinefähig',
      included: true,
    });
    req.flush(noteFixture({ title: 'Privatprojekt: Wetter-App' }));
    await fixture.whenStable();
    expect(el.querySelector('.editor')).toBeNull();
    expect(el.querySelectorAll('.note').length).toBe(1);
    expect(TestBed.inject(ToastService).message()).toBe('„Privatprojekt: Wetter-App“ gespeichert');
  });

  it('requires title and description', async () => {
    const { fixture, el } = await setup([]);
    (el.querySelector('.jc-page-head .jc-btn-primary') as HTMLButtonElement).click();
    await fixture.whenStable();
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(text(el.querySelector('.editor [role=alert]'))).toBe(
      'Bitte gib einen Titel und eine Beschreibung ein.',
    );
    http.expectNone('/api/notes');
  });

  it('edits a note in place', async () => {
    const { fixture, el } = await setup();
    (el.querySelector('.note .jc-btn-small') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect((el.querySelector('textarea') as HTMLTextAreaElement).value).toBe(
      'Angular 21 mit Signals, Spring Boot und PostgreSQL',
    );
    await type(fixture, el.querySelector('textarea'), 'Neu');
    (el.querySelector('button[type=submit]') as HTMLButtonElement).click();

    const req = http.expectOne('/api/notes/note-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.content).toBe('Neu');
    req.flush(noteFixture({ content: 'Neu' }));
    await fixture.whenStable();
    expect(text(el.querySelector('.note .content'))).toBe('Neu');
  });

  it('toggles whether the AI uses a note', async () => {
    const { fixture, el } = await setup();
    (el.querySelector('.include-pill') as HTMLButtonElement).click();
    const req = http.expectOne('/api/notes/note-1');
    expect(req.request.body.included).toBeFalse();
    req.flush(noteFixture({ included: false }));
    await fixture.whenStable();
    expect(text(el.querySelector('.include-pill'))).toBe('Nicht berücksichtigt');
    expect(TestBed.inject(NoteStore).includedNotes().length).toBe(0);
  });

  it('deletes a note after confirmation only', async () => {
    const { fixture, el } = await setup();
    const confirm = spyOn(TestBed.inject(ConfirmService), 'confirm').and.returnValues(
      of(false),
      of(true),
    );
    (el.querySelector('.jc-btn-ghost-danger') as HTMLButtonElement).click();
    http.expectNone('/api/notes/note-1');

    (el.querySelector('.jc-btn-ghost-danger') as HTMLButtonElement).click();
    expect(confirm).toHaveBeenCalledWith(jasmine.objectContaining({ danger: true }));
    http.expectOne({ method: 'DELETE', url: '/api/notes/note-1' }).flush(null);
    await fixture.whenStable();
    expect(el.querySelectorAll('.note').length).toBe(0);
  });
});
