import { Location } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApplicationService } from '../../core/api/application.service';
import { DocumentStore } from '../../core/api/document.store';
import { ApplicationDetail, LetterLength, Requirement, Tone } from '../../core/models/api.models';
import {
  CATEGORY_LABELS,
  LENGTH_OPTIONS,
  SAMPLE_URL,
  TONE_OPTIONS,
  URL_PATTERN,
  errorMessage,
} from '../../core/util/labels';

const ANALYSIS_STEPS = [
  'Seite abrufen',
  'HTML-Inhalt bereinigen',
  'Eckdaten extrahieren',
  'Mit deinem Profil abgleichen',
];
const STEP_LABELS = ['Stellenanzeige', 'Analyse', 'Prüfen & Kontext', 'Anschreiben'];
/** Mindestanzeigedauer je Analyseschritt, damit der Fortschritt nachvollziehbar bleibt. */
const STEP_PACE_MS = 650;

/** Neue Bewerbung: Stellen-URL → Analyse → Eckdaten prüfen & KI-Kontext wählen → Entwurf generieren. */
@Component({
  selector: 'app-new-application',
  imports: [FormsModule],
  templateUrl: './new-application.component.html',
  styleUrl: './new-application.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewApplicationComponent implements OnInit {
  private readonly api = inject(ApplicationService);
  private readonly documentStore = inject(DocumentStore);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly destroyRef = inject(DestroyRef);

  /** Bewerbungs-ID aus der Route (Fortsetzen nach Reload). */
  readonly id = input<string>();
  /** Stellen-URL aus der Übersicht (startet die Analyse sofort). */
  readonly url = input<string>();

  protected readonly analysisSteps = ANALYSIS_STEPS;
  protected readonly toneOptions = TONE_OPTIONS;
  protected readonly lengthOptions = LENGTH_OPTIONS;
  protected readonly categoryLabels = CATEGORY_LABELS;

  protected readonly step = signal<0 | 1 | 2>(0);
  protected readonly jobUrl = signal('');
  protected readonly urlError = signal('');
  protected readonly application = signal<ApplicationDetail | null>(null);
  protected readonly serverStep = signal(0);
  protected readonly shownStep = signal(0);
  protected readonly analysisError = signal('');

  protected readonly company = signal('');
  protected readonly role = signal('');
  protected readonly contact = signal('');
  protected readonly address = signal('');
  protected readonly requirements = signal<Requirement[]>([]);
  protected readonly tasks = signal<string[]>([]);
  protected readonly contextIds = signal<string[]>([]);
  protected readonly tone = signal<Tone>('FORMAL');
  protected readonly length = signal<LetterLength>('STANDARD');
  protected readonly generating = signal(false);
  protected readonly generateError = signal('');

  protected readonly readyDocuments = this.documentStore.readyDocuments;
  protected readonly matchLabel = computed(() => {
    const reqs = this.requirements();
    return `${reqs.filter((r) => r.matched).length} von ${reqs.length} Anforderungen erfüllt`;
  });
  protected readonly stepper = computed(() => {
    const current = this.step();
    return STEP_LABELS.map((label, i) => ({
      label,
      done: i < current,
      active: i === current,
    }));
  });

  private watch: Subscription | null = null;
  private pacer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => this.stopAnalysis());
  }

  ngOnInit(): void {
    const id = this.id();
    const url = this.url();
    if (id) {
      this.resume(id);
    } else if (url) {
      this.jobUrl.set(url);
      this.analyze();
    }
  }

  protected onUrlChange(value: string): void {
    this.jobUrl.set(value);
    this.urlError.set('');
  }

  protected useSample(): void {
    this.onUrlChange(SAMPLE_URL);
  }

  protected analyze(): void {
    const url = this.jobUrl().trim();
    if (!URL_PATTERN.test(url)) {
      this.urlError.set('Bitte gib einen gültigen Link ein (beginnend mit https://).');
      return;
    }
    this.stopAnalysis();
    this.urlError.set('');
    this.analysisError.set('');
    this.serverStep.set(0);
    this.shownStep.set(0);
    this.step.set(1);
    this.api
      .create(url)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (app) => {
          this.location.replaceState(`/bewerbung/neu/${app.id}`);
          this.follow(app.id);
        },
        error: (err: unknown) => {
          this.step.set(0);
          this.urlError.set(errorMessage(err, 'Die Analyse konnte nicht gestartet werden.'));
        },
      });
  }

  protected restart(): void {
    this.stopAnalysis();
    this.application.set(null);
    this.analysisError.set('');
    this.generateError.set('');
    this.step.set(0);
    this.location.replaceState('/bewerbung/neu');
  }

  protected toggleRequirement(index: number): void {
    this.requirements.update((reqs) =>
      reqs.map((r, i) => (i === index ? { ...r, matched: !r.matched } : r)),
    );
  }

  protected toggleContext(id: string): void {
    this.contextIds.update((ids) =>
      ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id],
    );
  }

  protected isInContext(id: string): boolean {
    return this.contextIds().includes(id);
  }

  protected generate(): void {
    const app = this.application();
    if (!app || this.generating()) {
      return;
    }
    if (!this.company().trim() || !this.role().trim()) {
      this.generateError.set('Bitte gib Unternehmen und Position an.');
      return;
    }
    this.generateError.set('');
    this.generating.set(true);
    this.api
      .generate(app.id, {
        companyName: this.company().trim(),
        jobTitle: this.role().trim(),
        contactPerson: this.contact().trim(),
        recipientAddress: this.address().trim(),
        requirements: this.requirements(),
        tasks: this.tasks(),
        documentIds: this.contextIds(),
        tone: this.tone(),
        length: this.length(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => void this.router.navigate(['/bewerbung', result.id]),
        error: (err: unknown) => {
          this.generating.set(false);
          this.generateError.set(
            errorMessage(err, 'Das Anschreiben konnte nicht erstellt werden.'),
          );
        },
      });
  }

  private resume(id: string): void {
    this.api
      .get(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (app) => {
          this.jobUrl.set(app.jobUrl);
          if (app.status === 'ANALYZING') {
            this.step.set(1);
            this.follow(app.id);
          } else if (app.status === 'ANALYSIS_FAILED') {
            this.step.set(1);
            this.shownStep.set(app.analysisStep);
            this.analysisError.set(app.errorMessage ?? 'Die Analyse ist fehlgeschlagen.');
          } else if (app.status === 'ANALYZED') {
            this.applyAnalysis(app);
            this.step.set(2);
          } else {
            void this.router.navigate(['/bewerbung', app.id], { replaceUrl: true });
          }
        },
        error: () => void this.router.navigate(['/bewerbung/neu'], { replaceUrl: true }),
      });
  }

  private follow(id: string): void {
    this.watch = this.api.watchAnalysis(id).subscribe({
      next: (app) => {
        this.application.set(app);
        this.serverStep.set(app.status === 'ANALYZED' ? ANALYSIS_STEPS.length : app.analysisStep);
        if (app.status === 'ANALYSIS_FAILED') {
          this.stopAnalysis();
          this.shownStep.set(app.analysisStep);
          this.analysisError.set(app.errorMessage ?? 'Die Analyse ist fehlgeschlagen.');
        }
      },
      error: (err: unknown) => {
        this.stopAnalysis();
        this.analysisError.set(errorMessage(err, 'Die Analyse ist fehlgeschlagen.'));
      },
    });
    this.pacer = setInterval(() => this.advance(), STEP_PACE_MS);
  }

  private advance(): void {
    if (this.shownStep() < this.serverStep()) {
      this.shownStep.update((s) => s + 1);
      return;
    }
    const app = this.application();
    if (this.shownStep() >= ANALYSIS_STEPS.length && app?.status === 'ANALYZED') {
      this.stopAnalysis();
      this.applyAnalysis(app);
      this.step.set(2);
    }
  }

  private applyAnalysis(app: ApplicationDetail): void {
    this.application.set(app);
    this.company.set(app.companyName ?? '');
    this.role.set(app.jobTitle ?? '');
    this.contact.set(app.contactPerson ?? '');
    this.address.set(app.recipientAddress ?? '');
    this.requirements.set(app.requirements);
    this.tasks.set(app.tasks);
    this.contextIds.set(app.contextDocumentIds);
    this.tone.set(app.tone);
    this.length.set(app.length);
  }

  private stopAnalysis(): void {
    this.watch?.unsubscribe();
    this.watch = null;
    if (this.pacer !== null) {
      clearInterval(this.pacer);
      this.pacer = null;
    }
  }
}
