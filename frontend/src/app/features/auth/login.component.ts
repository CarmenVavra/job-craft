import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { EMAIL_PATTERN, errorMessage } from '../../core/util/labels';
import { LogoComponent } from '../../shared/components/logo.component';

type AuthMode = 'login' | 'register';

/** Anmeldung und Registrierung (Split-Layout mit dunklem Markenpanel). */
@Component({
  selector: 'app-login',
  imports: [FormsModule, LogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly steps = [
    'Briefkopf hinterlegen',
    'Lebenslauf hochladen',
    'Stellenanzeige analysieren',
    'Anschreiben exportieren',
  ];

  protected readonly mode = signal<AuthMode>('login');
  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly passwordRepeat = signal('');
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly busy = signal(false);

  protected readonly isRegister = computed(() => this.mode() === 'register');
  protected readonly title = computed(() =>
    this.isRegister() ? 'Konto erstellen' : 'Willkommen zurück',
  );
  protected readonly subtitle = computed(() =>
    this.isRegister()
      ? 'In wenigen Minuten zur ersten maßgeschneiderten Bewerbung.'
      : 'Melde dich an, um deine Bewerbungen fortzusetzen.',
  );

  protected setMode(mode: AuthMode): void {
    this.mode.set(mode);
    this.error.set('');
    this.info.set('');
  }

  protected forgotPassword(): void {
    this.error.set('');
    this.info.set(
      'Das Zurücksetzen per E-Mail ist noch nicht verfügbar. Angemeldet kannst du dein Passwort jederzeit unter „Briefkopf“ ändern.',
    );
  }

  protected submit(): void {
    if (this.busy()) {
      return;
    }
    const email = this.email().trim();
    const password = this.password();
    if (!EMAIL_PATTERN.test(email)) {
      this.error.set('Bitte gib eine gültige E-Mail-Adresse ein.');
      return;
    }
    if (password.length < 8) {
      this.error.set('Das Passwort muss mindestens 8 Zeichen lang sein.');
      return;
    }
    if (this.isRegister() && !this.name().trim()) {
      this.error.set('Bitte gib deinen Namen ein.');
      return;
    }
    if (this.isRegister() && password !== this.passwordRepeat()) {
      this.error.set('Die beiden Passwörter stimmen nicht überein.');
      return;
    }
    this.error.set('');
    this.info.set('');
    this.busy.set(true);
    const request$ = this.isRegister()
      ? this.auth.register({ name: this.name().trim(), email, password })
      : this.auth.login({ email, password });
    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.password.set('');
        this.passwordRepeat.set('');
        void this.router.navigate(['/uebersicht']);
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(errorMessage(err, 'Die Anmeldung ist fehlgeschlagen.'));
      },
    });
  }
}
