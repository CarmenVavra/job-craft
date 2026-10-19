import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApplicationService } from '../../core/api/application.service';
import { DocumentStore } from '../../core/api/document.store';
import { ProfileStore } from '../../core/api/profile.store';
import { AuthService } from '../../core/auth/auth.service';
import { ApplicationStatus, ApplicationSummary } from '../../core/models/api.models';
import { ToastService } from '../../core/ui/toast.service';
import {
  SAMPLE_URL,
  SELECTABLE_STATUSES,
  STATUS_COLORS,
  STATUS_LABELS,
  URL_PATTERN,
  errorMessage,
  formatDate,
  profileCompleteness,
} from '../../core/util/labels';

/** Übersicht: neue Bewerbung starten, Briefkopf-/Dokumentstatus und alle Bewerbungen. */
@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'statusMenuFor.set(null)',
    '(document:keydown.escape)': 'statusMenuFor.set(null)',
  },
})
export class DashboardComponent implements OnInit {
  private readonly applicationsApi = inject(ApplicationService);
  private readonly profileStore = inject(ProfileStore);
  private readonly documentStore = inject(DocumentStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly selectableStatuses = SELECTABLE_STATUSES;
  protected readonly formatDate = formatDate;

  protected readonly applications = signal<ApplicationSummary[]>([]);
  protected readonly loaded = signal(false);
  protected readonly url = signal('');
  protected readonly urlError = signal('');
  protected readonly statusMenuFor = signal<string | null>(null);

  protected readonly firstName = computed(() => {
    const name = this.profileStore.profile()?.fullName ?? this.auth.user()?.name ?? '';
    return name.split(' ')[0];
  });
  protected readonly drafts = computed(
    () => this.applications().filter((a) => a.status === 'DRAFT').length,
  );
  protected readonly subtitle = computed(() => {
    if (this.applications().length === 0) {
      return 'Starte mit dem Link zu einer Stellenanzeige.';
    }
    const drafts = this.drafts();
    return drafts
      ? `${drafts} Anschreiben im Entwurf – mach weiter, wo du aufgehört hast.`
      : 'Alle Anschreiben sind versendet.';
  });
  protected readonly completeness = computed(() =>
    profileCompleteness(this.profileStore.profile()),
  );
  protected readonly documents = this.documentStore.documents;
  protected readonly docHint = computed(() => {
    const docs = this.documents();
    const ready = docs.filter((d) => d.status === 'READY').length;
    const cvs = docs.filter((d) => d.category === 'CV').length;
    return `${ready} mit extrahiertem Text · ${cvs} Lebenslauf`;
  });

  ngOnInit(): void {
    this.applicationsApi
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (apps) => {
          this.applications.set(apps);
          this.loaded.set(true);
        },
        error: (err: unknown) => {
          this.loaded.set(true);
          this.toast.show(errorMessage(err, 'Bewerbungen konnten nicht geladen werden.'));
        },
      });
  }

  protected onUrlChange(value: string): void {
    this.url.set(value);
    this.urlError.set('');
  }

  protected useSample(): void {
    this.onUrlChange(SAMPLE_URL);
  }

  protected analyze(): void {
    const url = this.url().trim();
    if (!URL_PATTERN.test(url)) {
      this.urlError.set('Bitte gib einen gültigen Link ein (beginnend mit https://).');
      return;
    }
    void this.router.navigate(['/bewerbung/neu'], { queryParams: { url } });
  }

  protected open(app: ApplicationSummary): void {
    void this.router.navigate(['/bewerbung', app.id]);
  }

  protected toggleStatusMenu(event: Event, id: string): void {
    event.stopPropagation();
    this.statusMenuFor.update((current) => (current === id ? null : id));
  }

  protected setStatus(event: Event, app: ApplicationSummary, status: ApplicationStatus): void {
    event.stopPropagation();
    this.statusMenuFor.set(null);
    if (status === app.status) {
      return;
    }
    this.applicationsApi
      .updateStatus(app.id, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.applications.update((apps) =>
            apps.map((a) => (a.id === app.id ? { ...a, status } : a)),
          );
          this.toast.show(`Status auf „${STATUS_LABELS[status]}“ gesetzt`);
        },
        error: (err: unknown) => this.toast.show(errorMessage(err)),
      });
  }
}
