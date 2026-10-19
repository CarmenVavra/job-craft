import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApplicationService } from '../../core/api/application.service';
import { ProfileStore } from '../../core/api/profile.store';
import { SystemService } from '../../core/api/system.service';
import { ApplicationDetail, CoverLetterVersion, LetterStyle } from '../../core/models/api.models';
import { ToastService } from '../../core/ui/toast.service';
import { LENGTH_OPTIONS, TONE_OPTIONS, errorMessage } from '../../core/util/labels';
import { LetterSheetComponent } from '../../shared/components/letter-sheet.component';
import { PdfExportDialogComponent, PdfExportDialogData } from './pdf-export-dialog.component';

/** Dauer der Hervorhebung geänderter Absätze. */
const HIGHLIGHT_MS = 2600;
const BASE_SUGGESTIONS = [
  'Java-Absatz prägnanter',
  'Soft Skills stärker betonen',
  'Englischkenntnisse erwähnen',
  'Einleitung kürzen',
];

interface ChatBubble {
  id: string;
  user: boolean;
  text: string;
  meta: string | null;
}

/** Anschreiben-Editor: A4-Vorschau mit Versionen, KI-Chat und PDF-Export. */
@Component({
  selector: 'app-editor',
  imports: [FormsModule, LetterSheetComponent],
  templateUrl: './editor.component.html',
  styleUrl: './editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorComponent implements OnInit {
  private readonly api = inject(ApplicationService);
  private readonly profileStore = inject(ProfileStore);
  private readonly system = inject(SystemService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Bewerbungs-ID aus der Route. */
  readonly id = input.required<string>();

  private readonly chatList = viewChild<ElementRef<HTMLElement>>('chatList');

  protected readonly application = signal<ApplicationDetail | null>(null);
  protected readonly current = signal(0);
  protected readonly draft = signal('');
  protected readonly typing = signal(false);
  protected readonly saving = signal(false);
  protected readonly pending = signal<string | null>(null);
  protected readonly highlighted = signal<number[]>([]);
  protected readonly letterStyle = signal<LetterStyle>('CLASSIC');

  protected readonly profile = this.profileStore.profile;
  protected readonly versions = computed(() => this.application()?.versions ?? []);
  protected readonly latestIndex = computed(() => this.versions().length - 1);
  protected readonly isOldVersion = computed(
    () => this.versions().length > 0 && this.current() !== this.latestIndex(),
  );
  protected readonly currentVersion = computed<CoverLetterVersion | null>(
    () => this.versions()[this.current()] ?? null,
  );
  protected readonly paragraphs = computed(() => this.currentVersion()?.paragraphs ?? []);
  protected readonly visibleHighlights = computed(() =>
    this.isOldVersion() ? [] : this.highlighted(),
  );
  protected readonly settingsLabel = computed(() => {
    const app = this.application();
    if (!app) return '';
    const tone = TONE_OPTIONS.find((t) => t.value === app.tone)?.label ?? '';
    const length = LENGTH_OPTIONS.find((l) => l.value === app.length)?.label ?? '';
    return `${tone} · ${length}`;
  });
  protected readonly demoMode = computed(() => this.system.info()?.aiEnabled === false);
  protected readonly bubbles = computed<ChatBubble[]>(() => {
    const list: ChatBubble[] = (this.application()?.messages ?? []).map((m) => ({
      id: m.id,
      user: m.role === 'USER',
      text: m.message,
      meta: m.role === 'USER' ? null : m.meta,
    }));
    const pending = this.pending();
    return pending ? [...list, { id: 'pending', user: true, text: pending, meta: null }] : list;
  });
  protected readonly suggestions = computed(() => {
    const gap = this.application()?.requirements.find((r) => !r.matched);
    return gap
      ? [...BASE_SUGGESTIONS, `${gap.text.split(/[\s/]/)[0]}-Lücke adressieren`]
      : BASE_SUGGESTIONS;
  });

  private highlightHandle: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    effect(() => {
      const profile = this.profile();
      if (profile) {
        this.letterStyle.set(profile.letterStyle);
      }
    });
    effect(() => {
      this.bubbles();
      this.typing();
      const el = this.chatList()?.nativeElement;
      if (el) {
        setTimeout(() => (el.scrollTop = el.scrollHeight));
      }
    });
    this.destroyRef.onDestroy(() => {
      if (this.highlightHandle !== null) clearTimeout(this.highlightHandle);
    });
  }

  ngOnInit(): void {
    this.api
      .get(this.id())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (app) => {
          if (app.versions.length === 0) {
            void this.router.navigate(['/bewerbung/neu', app.id], { replaceUrl: true });
            return;
          }
          this.application.set(app);
          this.current.set(app.versions.length - 1);
        },
        error: (err: unknown) => {
          this.toast.show(errorMessage(err, 'Bewerbung nicht gefunden.'));
          void this.router.navigate(['/uebersicht']);
        },
      });
  }

  protected back(): void {
    void this.router.navigate(['/uebersicht']);
  }

  protected showVersion(index: number): void {
    this.current.set(index);
  }

  protected toLatest(): void {
    this.current.set(this.latestIndex());
  }

  protected onDraftKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected send(text?: string): void {
    const app = this.application();
    const message = (text ?? this.draft()).trim();
    if (!app || !message || this.typing() || this.saving()) {
      return;
    }
    this.pending.set(message);
    this.draft.set('');
    this.typing.set(true);
    this.api
      .chat(app.id, message)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.pending.set(null);
          this.typing.set(false);
          this.application.set(updated);
          this.current.set(updated.versions.length - 1);
          this.highlight(updated.versions[updated.versions.length - 1]?.changedParagraphs ?? []);
        },
        error: (err: unknown) => {
          this.pending.set(null);
          this.typing.set(false);
          if (!text) {
            this.draft.set(message);
          }
          this.toast.show(errorMessage(err, 'Die KI konnte nicht antworten.'));
        },
      });
  }

  /** Ersetzt einen Absatz der aktuellen Version (neue Version). */
  protected saveParagraph(index: number, text: string): void {
    const paragraphs = [...this.paragraphs()];
    paragraphs[index] = text;
    this.saveParagraphs(paragraphs, 'Absatz gespeichert');
  }

  /** Entfernt einen Absatz der aktuellen Version (neue Version). */
  protected deleteParagraph(index: number): void {
    this.saveParagraphs(
      this.paragraphs().filter((_, i) => i !== index),
      `Absatz ${index + 1} gelöscht`,
    );
  }

  private saveParagraphs(paragraphs: string[], toast: string): void {
    const app = this.application();
    if (!app || this.saving() || this.isOldVersion()) {
      return;
    }
    this.saving.set(true);
    this.api
      .updateParagraphs(app.id, paragraphs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.application.set(updated);
          this.current.set(updated.versions.length - 1);
          this.highlight(updated.versions[updated.versions.length - 1]?.changedParagraphs ?? []);
          this.toast.show(toast);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.toast.show(errorMessage(err, 'Die Änderung konnte nicht gespeichert werden.'));
        },
      });
  }

  protected openPdf(): void {
    const app = this.application();
    const version = this.currentVersion();
    if (!app || !version) {
      return;
    }
    const data: PdfExportDialogData = {
      applicationId: app.id,
      version: version.version,
      company: app.companyName,
      fullName: this.profile()?.fullName ?? app.letter.senderName,
      letterStyle: this.letterStyle,
    };
    this.dialog.open(PdfExportDialogComponent, {
      data,
      width: '520px',
      maxWidth: 'calc(100vw - 48px)',
      panelClass: 'jc-dialog-panel',
      backdropClass: 'jc-dialog-backdrop',
      ariaLabelledBy: 'pdf-title',
    });
  }

  private highlight(indexes: number[]): void {
    this.highlighted.set(indexes);
    if (this.highlightHandle !== null) clearTimeout(this.highlightHandle);
    this.highlightHandle = setTimeout(() => this.highlighted.set([]), HIGHLIGHT_MS);
  }
}
