import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomField, LetterMeta, LetterStyle, Profile } from '../../core/models/api.models';

/**
 * Briefbogen mit Briefkopf (Klassisch/Modern). Variante „page“ zeigt das A4-Anschreiben im Editor,
 * Variante „preview“ die Briefkopf-Vorschau mit Platzhalter-Empfänger. Mit {@link editable} lassen sich
 * Absätze direkt bearbeiten oder löschen; gespeichert wird über die Ausgaben (eine neue Version je Änderung).
 */
@Component({
  selector: 'app-letter-sheet',
  imports: [FormsModule],
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
  /** Absätze direkt bearbeitbar (nur für die aktuelle Version). */
  readonly editable = input(false);
  /** Sperrt die Bearbeitung, z. B. während gespeichert wird. */
  readonly busy = input(false);
  /** Die Anrede wurde von Hand festgelegt (sonst automatisch aus der Ansprechperson). */
  readonly customSalutation = input(false);

  readonly paragraphSave = output<{ index: number; text: string }>();
  readonly paragraphDelete = output<number>();
  /** Neue Anrede; {@code null} = wieder automatisch. */
  readonly salutationSave = output<string | null>();

  protected readonly editingIndex = signal<number | null>(null);
  protected readonly draft = signal('');
  protected readonly editingSalutation = signal(false);
  protected readonly salutationDraft = signal('');

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

  constructor() {
    // Neue Version oder gesperrte Bearbeitung beendet einen offenen Bearbeitungsmodus.
    effect(() => {
      this.paragraphs();
      this.editable();
      this.editingIndex.set(null);
    });
    effect(() => {
      this.meta();
      this.editable();
      this.editingSalutation.set(false);
    });
  }

  protected isHighlighted(index: number): boolean {
    return this.highlighted().includes(index);
  }

  protected startEdit(index: number): void {
    this.draft.set(this.paragraphs()[index] ?? '');
    this.editingIndex.set(index);
  }

  protected cancelEdit(): void {
    this.editingIndex.set(null);
  }

  protected saveEdit(index: number): void {
    const text = this.draft().trim();
    if (!text) {
      return;
    }
    if (text === this.paragraphs()[index]) {
      this.editingIndex.set(null);
      return;
    }
    this.paragraphSave.emit({ index, text });
  }

  protected onEditKey(event: KeyboardEvent, index: number): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelEdit();
    } else if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      this.saveEdit(index);
    }
  }

  protected startSalutationEdit(): void {
    this.salutationDraft.set(this.meta()?.salutation ?? '');
    this.editingSalutation.set(true);
  }

  protected saveSalutation(): void {
    const text = this.salutationDraft().trim();
    if (!text) {
      return;
    }
    if (text === this.meta()?.salutation) {
      this.editingSalutation.set(false);
      return;
    }
    this.salutationSave.emit(text);
  }

  protected resetSalutation(): void {
    this.salutationSave.emit(null);
  }

  protected onSalutationKey(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.editingSalutation.set(false);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.saveSalutation();
    }
  }

  protected remove(index: number): void {
    this.paragraphDelete.emit(index);
  }
}

function nonBlank(value: string | null | undefined): value is string {
  return !!value && value.trim().length > 0;
}
