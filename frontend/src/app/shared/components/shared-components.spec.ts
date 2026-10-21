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

    it('lets the user edit and delete paragraphs when editable', async () => {
      const fixture = TestBed.createComponent(LetterSheetComponent);
      fixture.componentRef.setInput('paragraphs', ['Eins', 'Zwei']);
      fixture.componentRef.setInput('editable', true);
      const saved: { index: number; text: string }[] = [];
      const deleted: number[] = [];
      fixture.componentInstance.paragraphSave.subscribe((e) => saved.push(e));
      fixture.componentInstance.paragraphDelete.subscribe((i) => deleted.push(i));
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;

      (el.querySelectorAll('.paragraph-tools .danger')[1] as HTMLButtonElement).click();
      expect(deleted).toEqual([1]);

      (el.querySelectorAll('.paragraph-tools .tool')[0] as HTMLButtonElement).click();
      await fixture.whenStable();
      const area = el.querySelector('.edit-area') as HTMLTextAreaElement;
      area.value = 'Eins neu';
      area.dispatchEvent(new Event('input'));
      await fixture.whenStable();
      (el.querySelector('.edit-save') as HTMLButtonElement).click();
      expect(saved).toEqual([{ index: 0, text: 'Eins neu' }]);
    });

    it('edits the salutation and offers a reset when it was customised', async () => {
      const fixture = TestBed.createComponent(LetterSheetComponent);
      fixture.componentRef.setInput('meta', {
        senderName: '',
        senderLine: '',
        recipientLines: [],
        dateLine: '',
        subject: '',
        salutation: 'Sehr geehrte Frau Dr. Kessler,',
        closing: 'Mit freundlichen Grüßen',
      });
      fixture.componentRef.setInput('paragraphs', ['Eins']);
      fixture.componentRef.setInput('editable', true);
      const saved: (string | null)[] = [];
      fixture.componentInstance.salutationSave.subscribe((s) => saved.push(s));
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelectorAll('.salutation-tools .tool').length).toBe(1);

      (el.querySelector('.salutation-tools .tool') as HTMLButtonElement).click();
      await fixture.whenStable();
      const input = el.querySelector('.salutation-input') as HTMLInputElement;
      expect(input.value).toBe('Sehr geehrte Frau Dr. Kessler,');
      input.value = 'Liebe Frau Kessler,';
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
      expect(saved).toEqual(['Liebe Frau Kessler,']);

      fixture.componentRef.setInput('customSalutation', true);
      fixture.componentRef.setInput('meta', {
        ...fixture.componentInstance.meta()!,
        salutation: 'Liebe Frau Kessler,',
      });
      await fixture.whenStable();
      expect(el.querySelector('.salutation-input')).toBeNull();
      const tools = el.querySelectorAll('.salutation-tools .tool');
      expect(text(tools[1])).toBe('Automatisch');
      (tools[1] as HTMLButtonElement).click();
      expect(saved).toEqual(['Liebe Frau Kessler,', null]);
    });

    it('cancels editing with escape and hides tools when not editable', async () => {
      const fixture = TestBed.createComponent(LetterSheetComponent);
      fixture.componentRef.setInput('paragraphs', ['Eins']);
      fixture.componentRef.setInput('editable', true);
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;
      expect(
        (el.querySelector('.paragraph-tools .danger') as HTMLButtonElement).disabled,
      ).toBeTrue();

      (el.querySelector('.paragraph-tools .tool') as HTMLButtonElement).click();
      await fixture.whenStable();
      el.querySelector('.edit-area')?.dispatchEvent(
        new KeyboardEvent('keydown', { key: 'Escape' }),
      );
      await fixture.whenStable();
      expect(el.querySelector('.edit-area')).toBeNull();

      fixture.componentRef.setInput('editable', false);
      await fixture.whenStable();
      expect(el.querySelector('.paragraph-tools')).toBeNull();
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
