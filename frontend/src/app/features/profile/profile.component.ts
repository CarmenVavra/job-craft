import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ProfileStore } from '../../core/api/profile.store';
import { AuthService } from '../../core/auth/auth.service';
import { CustomField, LetterStyle, Profile } from '../../core/models/api.models';
import { ToastService } from '../../core/ui/toast.service';
import { errorMessage, formatLongDate } from '../../core/util/labels';
import { LetterSheetComponent } from '../../shared/components/letter-sheet.component';

/** Formularzustand der persönlichen Daten (PLZ & Ort in einem Feld wie im Mockup). */
interface ProfileForm {
  fullName: string;
  street: string;
  zipCity: string;
  phone: string;
  email: string;
  linkedin: string;
  github: string;
}

type ProfileField = keyof ProfileForm;

const EMPTY_FORM: ProfileForm = {
  fullName: '',
  street: '',
  zipCity: '',
  phone: '',
  email: '',
  linkedin: '',
  github: '',
};

/** Briefkopf & Stammdaten mit Live-Vorschau, eigenen Feldern und Passwortänderung. */
@Component({
  selector: 'app-profile',
  imports: [FormsModule, LetterSheetComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  private readonly store = inject(ProfileStore);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly fields: { key: ProfileField; label: string; wide: boolean; type: string }[] = [
    { key: 'fullName', label: 'Vollständiger Name', wide: true, type: 'text' },
    { key: 'street', label: 'Straße & Hausnummer', wide: false, type: 'text' },
    { key: 'zipCity', label: 'PLZ & Ort', wide: false, type: 'text' },
    { key: 'phone', label: 'Telefon', wide: false, type: 'tel' },
    { key: 'email', label: 'E-Mail', wide: false, type: 'email' },
    { key: 'linkedin', label: 'LinkedIn', wide: false, type: 'text' },
    { key: 'github', label: 'GitHub', wide: false, type: 'text' },
  ];
  protected readonly styleOptions: { value: LetterStyle; label: string }[] = [
    { value: 'CLASSIC', label: 'Klassisch' },
    { value: 'MODERN', label: 'Modern' },
  ];

  protected readonly form = signal<ProfileForm>(EMPTY_FORM);
  protected readonly customFields = signal<CustomField[]>([]);
  protected readonly letterStyle = signal<LetterStyle>('CLASSIC');
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly currentPassword = signal('');
  protected readonly newPassword = signal('');
  protected readonly confirmPassword = signal('');
  protected readonly passwordError = signal('');
  protected readonly changingPassword = signal(false);

  /** Vorschau-Profil aus dem aktuellen Formularzustand. */
  protected readonly previewProfile = computed<Profile>(() => this.toProfile());
  protected readonly dateLine = computed(() => {
    const city = splitZipCity(this.form().zipCity).city;
    const date = formatLongDate(new Date());
    return city ? `${city}, ${date}` : date;
  });

  constructor() {
    effect(() => {
      const profile = this.store.profile();
      if (profile) {
        untracked(() => this.fill(profile));
      }
    });
  }

  protected value(key: ProfileField): string {
    return this.form()[key];
  }

  protected update(key: ProfileField, value: string): void {
    this.form.update((f) => ({ ...f, [key]: value }));
  }

  protected addCustom(): void {
    this.customFields.update((fields) => [...fields, { label: '', value: '' }]);
  }

  protected updateCustom(index: number, key: keyof CustomField, value: string): void {
    this.customFields.update((fields) =>
      fields.map((f, i) => (i === index ? { ...f, [key]: value } : f)),
    );
  }

  protected removeCustom(index: number): void {
    this.customFields.update((fields) => fields.filter((_, i) => i !== index));
  }

  protected save(): void {
    if (!this.form().fullName.trim()) {
      this.error.set('Bitte gib deinen vollständigen Namen ein.');
      return;
    }
    this.error.set('');
    this.saving.set(true);
    this.store
      .save(this.toProfile())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.saving.set(false);
          this.auth.updateUserName(saved.fullName);
          this.toast.show('Briefkopf gespeichert');
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(errorMessage(err, 'Der Briefkopf konnte nicht gespeichert werden.'));
        },
      });
  }

  protected changePassword(): void {
    if (!this.currentPassword()) {
      this.passwordError.set('Bitte gib dein aktuelles Passwort ein.');
      return;
    }
    if (this.newPassword().length < 8) {
      this.passwordError.set('Das neue Passwort muss mindestens 8 Zeichen lang sein.');
      return;
    }
    if (this.newPassword() !== this.confirmPassword()) {
      this.passwordError.set('Die beiden neuen Passwörter stimmen nicht überein.');
      return;
    }
    this.passwordError.set('');
    this.changingPassword.set(true);
    this.auth
      .changePassword({ currentPassword: this.currentPassword(), newPassword: this.newPassword() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.changingPassword.set(false);
          this.currentPassword.set('');
          this.newPassword.set('');
          this.confirmPassword.set('');
          this.toast.show('Passwort geändert');
        },
        error: (err: unknown) => {
          this.changingPassword.set(false);
          this.passwordError.set(errorMessage(err, 'Das Passwort konnte nicht geändert werden.'));
        },
      });
  }

  private fill(profile: Profile): void {
    this.form.set({
      fullName: profile.fullName ?? '',
      street: profile.street ?? '',
      zipCity: [profile.zipCode, profile.city].filter(Boolean).join(' '),
      phone: profile.phone ?? '',
      email: profile.email ?? '',
      linkedin: profile.linkedin ?? '',
      github: profile.github ?? '',
    });
    this.customFields.set(profile.customFields.map((f) => ({ ...f })));
    this.letterStyle.set(profile.letterStyle);
  }

  private toProfile(): Profile {
    const f = this.form();
    const { zipCode, city } = splitZipCity(f.zipCity);
    return {
      fullName: f.fullName.trim(),
      street: f.street.trim() || null,
      zipCode,
      city,
      phone: f.phone.trim() || null,
      email: f.email.trim() || null,
      linkedin: f.linkedin.trim() || null,
      github: f.github.trim() || null,
      letterStyle: this.letterStyle(),
      customFields: this.customFields(),
    };
  }
}

/** Zerlegt „50674 Köln“ in PLZ und Ort. */
export function splitZipCity(value: string): { zipCode: string | null; city: string | null } {
  const trimmed = value.trim();
  const match = /^(\d{4,5})\s+(.+)$/.exec(trimmed);
  if (match) {
    return { zipCode: match[1], city: match[2].trim() };
  }
  if (/^\d{4,5}$/.test(trimmed)) {
    return { zipCode: trimmed, city: null };
  }
  return { zipCode: null, city: trimmed || null };
}
