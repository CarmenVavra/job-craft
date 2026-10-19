import { TestBed } from '@angular/core/testing';
import { profileFixture } from '../../../testing/fixtures';
import { testProviders, text } from '../../../testing/providers';
import { ToastService } from '../../core/ui/toast.service';
import { LetterSheetComponent } from './letter-sheet.component';
import { LogoComponent } from './logo.component';
import { ToastComponent } from './toast.component';

describe('shared components', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: testProviders() }));

  it('LogoComponent renders the brand', async () => {
    const fixture = TestBed.createComponent(LogoComponent);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toBe('JJobCraft');
  });

  it('ToastComponent shows the current toast', async () => {
    const fixture = TestBed.createComponent(ToastComponent);
    TestBed.inject(ToastService).show('Briefkopf gespeichert');
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('[role=status]'))).toBe(
      'Briefkopf gespeichert',
    );
  });

  describe('LetterSheetComponent', () => {
    it('renders the full letter with highlighted paragraph', async () => {
      const fixture = TestBed.createComponent(LetterSheetComponent);
      fixture.componentRef.setInput('profile', profileFixture());
      fixture.componentRef.setInput('meta', {
        senderName: 'Lena Hoffmann',
        senderLine: 'Lena Hoffmann · Lindenstraße 12 · 50674 Köln',
        recipientLines: ['Quantis Software GmbH', 'Frau Dr. Miriam Kessler'],
        dateLine: 'Köln, 19. Oktober 2026',
        subject: 'Bewerbung als Senior Fullstack Entwickler',
        salutation: 'Sehr geehrte Frau Dr. Kessler,',
        closing: 'Mit freundlichen Grüßen',
      });
      fixture.componentRef.setInput('paragraphs', ['Eins', 'Zwei']);
      fixture.componentRef.setInput('highlighted', [1]);
      await fixture.whenStable();

      const el: HTMLElement = fixture.nativeElement;
      expect(text(el.querySelector('.subject'))).toBe('Bewerbung als Senior Fullstack Entwickler');
      expect(text(el.querySelector('.salutation'))).toBe('Sehr geehrte Frau Dr. Kessler,');
      expect(el.querySelectorAll('.paragraph').length).toBe(2);
      expect(el.querySelector('.paragraph.changed')?.textContent).toBe('Zwei');
      expect(el.querySelector('.head-classic')).not.toBeNull();
      expect(text(el.querySelector('.custom'))).toContain('Führerschein: Klasse B');
    });

    it('renders the modern letterhead with placeholders in preview mode', async () => {
      const fixture = TestBed.createComponent(LetterSheetComponent);
      fixture.componentRef.setInput('profile', profileFixture());
      fixture.componentRef.setInput('letterStyle', 'MODERN');
      fixture.componentRef.setInput('variant', 'preview');
      fixture.componentRef.setInput('dateLine', 'Köln, 1. Januar 2027');
      await fixture.whenStable();

      const el: HTMLElement = fixture.nativeElement;
      expect(text(el.querySelector('.head-modern .address'))).toBe('Lindenstraße 12 · 50674 Köln');
      expect(text(el.querySelector('.placeholder'))).toContain('Ansprechpartner');
      expect(text(el.querySelector('.preview-date'))).toBe('Köln, 1. Januar 2027');
    });
  });
});
