import { HttpErrorResponse } from '@angular/common/http';
import {
  ApiError,
  ApplicationStatus,
  DocumentCategory,
  LetterLength,
  Profile,
  Tone,
} from '../models/api.models';

/** Farbpaar (Hintergrund, Text) für Pills. */
export interface PillColors {
  bg: string;
  fg: string;
}

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  ANALYZING: 'Analyse',
  ANALYSIS_FAILED: 'Fehler',
  ANALYZED: 'Analysiert',
  DRAFT: 'Entwurf',
  SENT: 'Versendet',
  INTERVIEW: 'Gespräch',
  REJECTED: 'Abgesagt',
};

export const STATUS_COLORS: Record<ApplicationStatus, PillColors> = {
  ANALYZING: { bg: '#ECEBE4', fg: '#55544C' },
  ANALYSIS_FAILED: { bg: '#F8E4E4', fg: '#AF1C1C' },
  ANALYZED: { bg: '#ECEBE4', fg: '#55544C' },
  DRAFT: { bg: '#ECEBE4', fg: '#55544C' },
  SENT: { bg: '#DCEFF1', fg: '#1E6E75' },
  INTERVIEW: { bg: '#2F2F28', fg: '#FFFFFF' },
  REJECTED: { bg: '#F8E4E4', fg: '#AF1C1C' },
};

/** Status, die in der Übersicht manuell gesetzt werden können. */
export const SELECTABLE_STATUSES: ApplicationStatus[] = ['DRAFT', 'SENT', 'INTERVIEW', 'REJECTED'];

export const CATEGORY_LABELS: Record<DocumentCategory, string> = {
  CV: 'Lebenslauf',
  SKILLS: 'Skill-Profil',
  CERTIFICATE: 'Zeugnis',
  OTHER: 'Sonstiges',
};

export const CATEGORY_COLORS: Record<DocumentCategory, PillColors> = {
  CV: { bg: '#DCEFF1', fg: '#1E6E75' },
  SKILLS: { bg: '#E4E2FA', fg: '#3B3878' },
  CERTIFICATE: { bg: '#ECEBE4', fg: '#55544C' },
  OTHER: { bg: '#F8E4E4', fg: '#AF1C1C' },
};

export const CATEGORIES: DocumentCategory[] = ['CV', 'SKILLS', 'CERTIFICATE', 'OTHER'];

export const TONE_OPTIONS: { value: Tone; label: string }[] = [
  { value: 'FORMAL', label: 'Formell' },
  { value: 'MODERN', label: 'Modern' },
  { value: 'CASUAL', label: 'Locker' },
];

export const LENGTH_OPTIONS: { value: LetterLength; label: string }[] = [
  { value: 'COMPACT', label: 'Kompakt' },
  { value: 'STANDARD', label: 'Standard' },
  { value: 'DETAILED', label: 'Ausführlich' },
];

/** E-Mail-Prüfung wie im Prototyp. */
export const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

/** URL-Prüfung für Stellenanzeigen. */
export const URL_PATTERN = /^https?:\/\/\S+\.\S+/;

/** Beispiel-Link aus dem Prototyp (liefert im Backend eine Demo-Anzeige). */
export const SAMPLE_URL = 'https://karriere.quantis-software.de/jobs/senior-fullstack-entwickler';

/** Entfernt Gender-Zusätze wie „(m/w/d)“. */
export function cleanRole(role: string | null | undefined): string {
  return (role ?? '')
    .replace(/\s*\((?:m|w|d|f|x|i|div)(?:\s*\/\s*(?:m|w|d|f|x|i|div))+\)/gi, '')
    .replace(/\s*\(all genders?\)/gi, '')
    .trim();
}

/** Dateigröße im deutschen Format, z. B. „248 KB“ oder „1,1 MB“. */
export function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
}

/** Datum als TT.MM.JJJJ. */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

/** Langes Datum, z. B. „19. Oktober 2026“. */
export function formatLongDate(date: Date): string {
  return date.toLocaleDateString('de-DE', { day: 'numeric', month: 'long', year: 'numeric' });
}

/** Die sieben Briefkopf-Felder, aus denen sich die Vollständigkeit berechnet. */
export function profileCompleteness(profile: Profile | null): { percent: number; missing: number } {
  if (!profile) {
    return { percent: 0, missing: 7 };
  }
  const values = [
    profile.fullName,
    profile.street,
    [profile.zipCode, profile.city].filter(Boolean).join(' '),
    profile.phone,
    profile.email,
    profile.linkedin,
    profile.github,
  ];
  const filled = values.filter((v) => !!v && v.trim().length > 0).length;
  return { percent: Math.round((filled / values.length) * 100), missing: values.length - filled };
}

/** Initialen für den Avatar. */
export function initials(name: string | null | undefined): string {
  return (name ?? '')
    .split(/\s+/)
    .map((w) => w[0] ?? '')
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

/** Dateiname des PDF-Exports: Bewerbung_{Unternehmen}_{Nachname}.pdf */
export function pdfFileName(company: string | null, fullName: string | null): string {
  const c = (company ?? '').replace(/[^A-Za-z0-9ÄÖÜäöüß]+/g, '_').replace(/^_+|_+$/g, '');
  const parts = (fullName ?? '').trim().split(/\s+/);
  const last = (parts[parts.length - 1] ?? '').replace(/[^A-Za-z0-9ÄÖÜäöüß-]+/g, '');
  return `Bewerbung${c ? '_' + c : ''}${last ? '_' + last : ''}.pdf`;
}

/** Liest die Fehlermeldung aus einer API-Antwort. */
export function errorMessage(error: unknown, fallback = 'Es ist ein Fehler aufgetreten.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Der Server ist nicht erreichbar.';
    }
    const body = error.error as Partial<ApiError> | null;
    if (body && typeof body.message === 'string' && body.message.length > 0) {
      return body.message;
    }
  }
  return fallback;
}
