import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { DocumentStore } from '../../core/api/document.store';
import { NoteStore } from '../../core/api/note.store';
import { ProfileStore } from '../../core/api/profile.store';
import { SystemService } from '../../core/api/system.service';
import { AuthService } from '../../core/auth/auth.service';
import { initials } from '../../core/util/labels';
import { LogoComponent } from '../components/logo.component';
import { ToastComponent } from '../components/toast.component';

interface NavItem {
  id: 'dashboard' | 'new' | 'documents' | 'about' | 'profile';
  label: string;
  link: string;
}

/** App-Rahmen: dunkle Sidebar mit Navigation und Benutzer, rechts der scrollbare Inhalt. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, LogoComponent, ToastComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly profileStore = inject(ProfileStore);
  private readonly documentStore = inject(DocumentStore);
  private readonly noteStore = inject(NoteStore);
  private readonly system = inject(SystemService);

  protected readonly nav: NavItem[] = [
    { id: 'dashboard', label: 'Übersicht', link: '/uebersicht' },
    { id: 'new', label: 'Neue Bewerbung', link: '/bewerbung/neu' },
    { id: 'documents', label: 'Dokumente', link: '/dokumente' },
    { id: 'about', label: 'Über mich', link: '/ueber-mich' },
    { id: 'profile', label: 'Briefkopf', link: '/briefkopf' },
  ];

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** Der Editor zählt als „Übersicht“. */
  protected readonly active = computed<NavItem['id']>(() => {
    const url = this.url();
    if (url.startsWith('/bewerbung/neu')) return 'new';
    if (url.startsWith('/dokumente')) return 'documents';
    if (url.startsWith('/ueber-mich')) return 'about';
    if (url.startsWith('/briefkopf')) return 'profile';
    return 'dashboard';
  });

  protected readonly name = computed(
    () => this.profileStore.profile()?.fullName ?? this.auth.user()?.name ?? '',
  );
  protected readonly email = computed(
    () => this.profileStore.profile()?.email ?? this.auth.user()?.email ?? '',
  );
  protected readonly initials = computed(() => initials(this.name()));

  ngOnInit(): void {
    this.profileStore.load().subscribe({ error: () => undefined });
    this.documentStore.load().subscribe({ error: () => undefined });
    this.noteStore.load().subscribe({ error: () => undefined });
    this.system.load();
  }

  protected logout(): void {
    this.auth.logout();
  }
}
