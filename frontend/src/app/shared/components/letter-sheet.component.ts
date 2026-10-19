import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CustomField, LetterMeta, LetterStyle, Profile } from '../../core/models/api.models';

/**
 * Briefbogen mit Briefkopf (Klassisch/Modern). Variante „page“ zeigt das A4-Anschreiben im Editor,
 * Variante „preview“ die Briefkopf-Vorschau mit Platzhalter-Empfänger.
 */
@Component({
  selector: 'app-letter-sheet',
  templateUrl: './letter-sheet.component.html',
  styleUrl: './letter-sheet.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.preview]': "variant() === 'preview'", '[class.page]': "variant() === 'page'" },
})
export class LetterSheetComponent {
  readonly profile = input<Profile | null>(null);
  readonly letterStyle = input<LetterStyle>('CLASSIC');
  readonly meta = input<LetterMeta | null>(null);
  readonly paragraphs = input<string[]>([]);
  readonly highlighted = input<number[]>([]);
  readonly variant = input<'page' | 'preview'>('page');
  readonly dateLine = input<string>('');

  protected readonly modern = computed(() => this.letterStyle() === 'MODERN');
  protected readonly name = computed(() => this.profile()?.fullName ?? '');
  protected readonly contactLines = computed(() => {
    const p = this.profile();
    return p ? [p.phone, p.email, p.linkedin, p.github].filter(nonBlank) : [];
  });
  protected readonly zipCity = computed(() => {
    const p = this.profile();
    return p ? [p.zipCode, p.city].filter(nonBlank).join(' ') : '';
  });
  protected readonly modernAddress = computed(() =>
    [this.profile()?.street, this.zipCity()].filter(nonBlank).join(' · '),
  );
  protected readonly senderLine = computed(
    () =>
      this.meta()?.senderLine ||
      [this.name(), this.profile()?.street, this.zipCity()].filter(nonBlank).join(' · '),
  );
  protected readonly customFields = computed<CustomField[]>(() =>
    (this.profile()?.customFields ?? []).filter((c) => nonBlank(c.label) && nonBlank(c.value)),
  );
  protected readonly date = computed(() => this.meta()?.dateLine ?? this.dateLine());

  protected isHighlighted(index: number): boolean {
    return this.highlighted().includes(index);
  }
}

function nonBlank(value: string | null | undefined): value is string {
  return !!value && value.trim().length > 0;
}
